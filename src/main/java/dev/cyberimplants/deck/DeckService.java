package dev.cyberimplants.deck;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.network.DeckStatusPayload;
import dev.cyberimplants.registry.ModAttachments;
import dev.cyberimplants.synapse.SynapseException;
import dev.cyberimplants.synapse.compiler.CompiledProgram;
import dev.cyberimplants.synapse.compiler.Compiler;
import dev.cyberimplants.synapse.vm.ProgramRunner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side deck management: storing programs, booting one (Layer 1 runs a single program), ticking it within the
 * Processor's cycle budget and turning game happenings into program events.
 */
public final class DeckService {
    /** 1 Charge per this many executed cycles (docs/design/03-cyberdeck.md, section 3.4). */
    private static final double CYCLES_PER_CHARGE = 2000.0;
    private static final int MACRO_KEYS = 2;

    private static final Map<UUID, DeckRuntime> RUNTIMES = new ConcurrentHashMap<>();

    public static DeckRuntime runtime(ServerPlayer player) {
        return RUNTIMES.get(player.getUUID());
    }

    // ---------------------------------------------------------------- storing programs

    /** Compiles and stores a program; reports the outcome to the player's deck screen. */
    public static void upload(ServerPlayer player, String source) {
        DeckHardware hw = DeckHardware.of(BodyState.get(player));
        if (!hw.booted()) {
            sendStatus(player, "No bootable deck: install a chassis, a Processor, RAM and Storage module.");
            return;
        }
        if (source.length() > ServerConfig.DECK_MAX_SOURCE_LENGTH.get()) {
            sendStatus(player, "Program too long (limit " + ServerConfig.DECK_MAX_SOURCE_LENGTH.get() + " characters).");
            return;
        }
        CompiledProgram program;
        try {
            program = Compiler.compile(source, hw.isa(), DeckAliases.names());
        } catch (SynapseException e) {
            sendStatus(player, "Compile error at " + e.getMessage());
            return;
        }
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        DeckData next = data.withProgram(program.name(), source);
        if (next.storedBytes() > hw.storageBytes()) {
            sendStatus(player, "Storage full (" + next.storedBytes() + " / " + hw.storageBytes() + " bytes).");
            return;
        }
        player.setData(ModAttachments.DECK_DATA, next);
        String message = "Saved '" + program.name() + "' (" + program.footprintCells() + " memory cells, ISA-"
                + program.isa().number() + ")";
        DeckRuntime running = runtime(player);
        if (running != null && running.programName().equals(program.name())) {
            String error = boot(player, program.name());
            message += error == null ? "; reloaded." : "; reload failed: " + error;
        }
        sendStatus(player, message);
    }

    public static void deleteCommand(ServerPlayer player, String name) {
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        DeckRuntime rt = runtime(player);
        if (rt != null && rt.programName().equals(name)) {
            shutdown(player);
        }
        player.setData(ModAttachments.DECK_DATA, data.withoutProgram(name));
        sendStatus(player, "Deleted '" + name + "'.");
    }

    // ---------------------------------------------------------------- running programs

    public static void loadCommand(ServerPlayer player, String name) {
        String error = boot(player, name);
        sendStatus(player, error == null ? "Loaded '" + name + "'." : error);
    }

    public static void unloadCommand(ServerPlayer player) {
        shutdown(player);
        player.setData(ModAttachments.DECK_DATA, player.getData(ModAttachments.DECK_DATA).withLoaded(""));
        sendStatus(player, "Program unloaded.");
    }

    /** Compiles and starts a stored program. Returns an error message or null. */
    private static String boot(ServerPlayer player, String name) {
        DeckHardware hw = DeckHardware.of(BodyState.get(player));
        if (!hw.booted()) {
            return "No bootable deck.";
        }
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        String source = data.programs().get(name);
        if (source == null) {
            return "No stored program '" + name + "'.";
        }
        CompiledProgram program;
        try {
            program = Compiler.compile(source, hw.isa(), DeckAliases.names());
        } catch (SynapseException e) {
            return "Compile error at " + e.getMessage();
        }
        if (program.footprintCells() > hw.ramCells()) {
            return "Not enough RAM: needs " + program.footprintCells() + " cells, deck has " + hw.ramCells() + ".";
        }
        shutdown(player);
        DeckRuntime[] holder = new DeckRuntime[1];
        DeckHost host = new DeckHost(player, () -> holder[0]);
        ProgramRunner runner = new ProgramRunner(program, host);
        DeckRuntime runtime = new DeckRuntime(name, runner, player.getUUID().getLeastSignificantBits() ^ player.level().getGameTime(), 20);
        runtime.bootTick = player.level().getGameTime();
        holder[0] = runtime;
        RUNTIMES.put(player.getUUID(), runtime);
        player.setData(ModAttachments.DECK_DATA, data.withLoaded(name));
        runner.start();
        return null;
    }

    /** Stops the running program (if any) without changing which program is marked loaded. */
    public static void shutdown(ServerPlayer player) {
        DeckRuntime old = RUNTIMES.remove(player.getUUID());
        if (old != null) {
            old.runner().stop();
        }
    }

    /** Called when the body changes, on login and on respawn: reboots the loaded program or stops it if hardware is gone. */
    public static void onBodyChanged(ServerPlayer player) {
        DeckHardware hw = DeckHardware.of(BodyState.get(player));
        DeckRuntime rt = runtime(player);
        if (!hw.booted()) {
            shutdown(player);
            return;
        }
        if (rt != null && rt.runner().program().footprintCells() > hw.ramCells()) {
            shutdown(player);
            return;
        }
        if (rt == null) {
            String loaded = player.getData(ModAttachments.DECK_DATA).loaded();
            if (!loaded.isEmpty()) {
                String error = boot(player, loaded);
                if (error != null) {
                    CyberImplants.LOGGER.debug("Deck of {} did not boot: {}", player.getGameProfile().getName(), error);
                }
            }
        }
    }

    // ---------------------------------------------------------------- per-tick

    public static void tick(ServerPlayer player) {
        DeckRuntime rt = RUNTIMES.get(player.getUUID());
        if (rt == null || !player.isAlive() || rt.runner().state() != ProgramRunner.State.RUNNING) {
            return;
        }
        BodyState body = BodyState.get(player);
        DeckHardware hw = DeckHardware.of(body);
        pollEvents(player, rt, body);

        // Computation costs Charge; a drained battery stalls the deck.
        boolean powered = !Body.chargeEnabled() || body.charge() > 0;
        int used = powered ? rt.runner().tick(hw.cyclesPerTick()) : 0;
        rt.lastCycles = used;
        if (Body.chargeEnabled() && used > 0) {
            rt.cycleDebt += used;
            while (rt.cycleDebt >= CYCLES_PER_CHARGE) {
                rt.cycleDebt -= CYCLES_PER_CHARGE;
                Body.spend(player, 1.0);
            }
        }
    }

    /** Events the program can only learn about by watching the player: thresholds and ready notifications. */
    private static void pollEvents(ServerPlayer player, DeckRuntime rt, BodyState body) {
        ProgramRunner runner = rt.runner();
        if (runner.subscribes("implant_ready")) {
            for (ResourceLocation id : body.cooldownEnd().keySet()) {
                if (body.cooldownRemaining(id) > 0) {
                    rt.cooling.add(id);
                }
            }
            for (ResourceLocation id : new ArrayList<>(rt.cooling)) {
                if (body.cooldownRemaining(id) == 0) {
                    rt.cooling.remove(id);
                    if (body.has(id)) {
                        runner.fire("implant_ready", null, Map.of("implant", new ImplantHandle(player, id)));
                    }
                }
            }
        }
        if (body.has(ImplantIds.BIOMONITOR) && (runner.subscribes("health_below") || runner.subscribes("health_above"))) {
            double pct = player.getHealth() / player.getMaxHealth() * 100.0;
            if (!Double.isNaN(rt.prevHealthPct)) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("health", (double) player.getHealth());
                payload.put("health_pct", pct);
                for (Double t : runner.arguments("health_below")) {
                    if (rt.prevHealthPct >= t && pct < t) {
                        runner.fire("health_below", t, payload);
                    }
                }
                for (Double t : runner.arguments("health_above")) {
                    if (rt.prevHealthPct < t && pct >= t) {
                        runner.fire("health_above", t, payload);
                    }
                }
            }
            rt.prevHealthPct = pct;
        }
        if (runner.subscribes("charge_below")) {
            double charge = body.charge();
            if (!Double.isNaN(rt.prevCharge)) {
                for (Double t : runner.arguments("charge_below")) {
                    if (rt.prevCharge >= t && charge < t) {
                        runner.fire("charge_below", t, Map.of("charge", charge));
                    }
                }
            }
            rt.prevCharge = charge;
        }
    }

    public static void macroKey(ServerPlayer player, int key) {
        DeckRuntime rt = runtime(player);
        if (rt != null && key >= 1 && key <= MACRO_KEYS) {
            rt.runner().fire("key", (double) key, Map.of("n", (double) key));
        }
    }

    /** Records the outcome of a scripted action so programs can read {@code last_error}. */
    public static void recordResult(ServerPlayer player, UseResult result) {
        DeckRuntime rt = runtime(player);
        if (rt != null && !result.ok()) {
            rt.lastError = result.name();
        }
    }

    // ---------------------------------------------------------------- status for the screen

    public static void sendStatus(ServerPlayer player, String message) {
        BodyState body = BodyState.get(player);
        DeckHardware hw = DeckHardware.of(body);
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        DeckRuntime rt = runtime(player);

        List<String> names = new ArrayList<>(data.programs().keySet());
        names.sort(String::compareToIgnoreCase);
        List<String> sources = new ArrayList<>();
        for (String name : names) {
            sources.add(data.programs().get(name));
        }
        String loaded = rt == null ? "" : rt.programName();
        int footprint = rt == null ? 0 : rt.runner().program().footprintCells();
        List<String> log = rt == null ? List.of() : rt.logLines();
        String status = message;
        if (rt != null && rt.runner().state() == ProgramRunner.State.CRASHED) {
            status = (message.isEmpty() ? "" : message + " ") + "Program crashed: " + rt.runner().lastError();
        }
        PacketDistributor.sendToPlayer(player, new DeckStatusPayload(hw.booted(), hw.isa().number(), hw.ramCells(),
                hw.storageBytes(), data.storedBytes(), names, sources, loaded, footprint, log, status));
    }

    private DeckService() {
    }
}
