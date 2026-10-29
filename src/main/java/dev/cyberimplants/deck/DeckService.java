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
 * Server-side deck management: storing programs, booting them (one per thread: the Processor's thread 0 plus one per
 * Coprocessor thread), ticking each within its cycle budget and turning game happenings into program events.
 */
public final class DeckService {
    /** 1 Charge per this many executed cycles (docs/design/03-cyberdeck.md, section 3.4). */
    private static final double CYCLES_PER_CHARGE = 2000.0;

    /** Per player: thread number -> the program running on it. */
    private static final Map<UUID, Map<Integer, DeckRuntime>> RUNTIMES = new ConcurrentHashMap<>();

    /** The program on the lowest running thread, or null. */
    public static DeckRuntime runtime(ServerPlayer player) {
        Map<Integer, DeckRuntime> threads = RUNTIMES.get(player.getUUID());
        return threads == null || threads.isEmpty() ? null : threads.values().iterator().next();
    }

    /** Every running program, thread 0 first. */
    public static List<DeckRuntime> runtimes(ServerPlayer player) {
        Map<Integer, DeckRuntime> threads = RUNTIMES.get(player.getUUID());
        return threads == null ? List.of() : List.copyOf(threads.values());
    }

    private static DeckRuntime runtimeAt(ServerPlayer player, int thread) {
        Map<Integer, DeckRuntime> threads = RUNTIMES.get(player.getUUID());
        return threads == null ? null : threads.get(thread);
    }

    private static Compiler.Limits limits(DeckHardware hw) {
        return new Compiler.Limits(hw.minEveryTicks());
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
            program = Compiler.compile(source, hw.isa(), DeckAliases.names(), limits(hw));
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
        for (DeckRuntime running : runtimes(player)) {
            if (running.programName().equals(program.name())) {
                String error = boot(player, program.name(), running.thread());
                message += error == null ? "; reloaded." : "; reload failed: " + error;
            }
        }
        sendStatus(player, message);
    }

    public static void deleteCommand(ServerPlayer player, String name) {
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        for (DeckRuntime rt : runtimes(player)) {
            if (rt.programName().equals(name)) {
                shutdown(player, rt.thread());
            }
        }
        player.setData(ModAttachments.DECK_DATA, data.withoutProgram(name));
        sendStatus(player, "Deleted '" + name + "'.");
    }

    // ---------------------------------------------------------------- running programs

    public static void loadCommand(ServerPlayer player, String name, int thread) {
        String error = boot(player, name, thread);
        sendStatus(player, error == null ? "Loaded '" + name + "' on thread " + thread + "." : error);
    }

    /** Unloads one thread, or every thread when {@code thread} is negative. */
    public static void unloadCommand(ServerPlayer player, int thread) {
        for (DeckRuntime rt : runtimes(player)) {
            if (thread < 0 || rt.thread() == thread) {
                shutdown(player, rt.thread());
                player.setData(ModAttachments.DECK_DATA, player.getData(ModAttachments.DECK_DATA).withLoaded(rt.thread(), ""));
            }
        }
        sendStatus(player, "Program unloaded.");
    }

    /** RAM the other running programs hold, which this one cannot use. */
    private static int ramInUse(ServerPlayer player, int exceptThread) {
        int total = 0;
        for (DeckRuntime rt : runtimes(player)) {
            if (rt.thread() != exceptThread) {
                total += rt.runner().program().footprintCells();
            }
        }
        return total;
    }

    /** Compiles and starts a stored program on a thread. Returns an error message or null. */
    private static String boot(ServerPlayer player, String name, int thread) {
        DeckHardware hw = DeckHardware.of(BodyState.get(player));
        if (!hw.booted()) {
            return "No bootable deck.";
        }
        if (thread < 0 || thread >= hw.threads()) {
            return "This deck has no thread " + thread + " (install a Coprocessor).";
        }
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        String source = data.programs().get(name);
        if (source == null) {
            return "No stored program '" + name + "'.";
        }
        for (DeckRuntime other : runtimes(player)) {
            if (other.thread() != thread && other.programName().equals(name)) {
                return "'" + name + "' already runs on thread " + other.thread() + ".";
            }
        }
        CompiledProgram program;
        try {
            program = Compiler.compile(source, hw.isa(), DeckAliases.names(), limits(hw));
        } catch (SynapseException e) {
            return "Compile error at " + e.getMessage();
        }
        int freeRam = hw.ramCells() - ramInUse(player, thread);
        if (program.footprintCells() > freeRam) {
            return "Not enough RAM: needs " + program.footprintCells() + " cells, " + freeRam + " free.";
        }
        shutdown(player, thread);
        DeckRuntime[] holder = new DeckRuntime[1];
        DeckHost host = new DeckHost(player, () -> holder[0]);
        ProgramRunner runner = new ProgramRunner(program, host, freeRam);
        DeckRuntime runtime = new DeckRuntime(name, thread, runner,
                player.getUUID().getLeastSignificantBits() ^ player.level().getGameTime() ^ thread, hw.logLines());
        runtime.bootTick = player.level().getGameTime();
        holder[0] = runtime;
        RUNTIMES.computeIfAbsent(player.getUUID(), id -> new java.util.concurrent.ConcurrentSkipListMap<>()).put(thread, runtime);
        player.setData(ModAttachments.DECK_DATA, data.withLoaded(thread, name));
        runner.start();
        return null;
    }

    /** Stops the program on a thread without changing which program is marked loaded there. */
    private static void shutdown(ServerPlayer player, int thread) {
        Map<Integer, DeckRuntime> threads = RUNTIMES.get(player.getUUID());
        DeckRuntime old = threads == null ? null : threads.remove(thread);
        if (old != null) {
            old.runner().stop();
        }
    }

    /** Stops every program (logout, respawn, hardware gone). */
    public static void shutdown(ServerPlayer player) {
        Map<Integer, DeckRuntime> old = RUNTIMES.remove(player.getUUID());
        if (old != null) {
            old.values().forEach(rt -> rt.runner().stop());
        }
    }

    /** Called when the body changes, on login and on respawn: reboots loaded programs or stops them if hardware is gone. */
    public static void onBodyChanged(ServerPlayer player) {
        DeckHardware hw = DeckHardware.of(BodyState.get(player));
        if (!hw.booted()) {
            shutdown(player);
            return;
        }
        int used = 0;
        for (DeckRuntime rt : runtimes(player)) {
            if (rt.thread() >= hw.threads() || used + rt.runner().program().footprintCells() > hw.ramCells()) {
                shutdown(player, rt.thread());
            } else {
                used += rt.runner().program().footprintCells();
            }
        }
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        for (int thread = 0; thread < hw.threads(); thread++) {
            String loaded = data.loadedAt(thread);
            if (runtimeAt(player, thread) == null && !loaded.isEmpty()) {
                String error = boot(player, loaded, thread);
                if (error != null) {
                    CyberImplants.LOGGER.debug("Deck thread {} of {} did not boot: {}", thread, player.getGameProfile().getName(), error);
                }
            }
        }
    }

    // ---------------------------------------------------------------- per-tick

    public static void tick(ServerPlayer player) {
        Map<Integer, DeckRuntime> threads = RUNTIMES.get(player.getUUID());
        if (threads == null || threads.isEmpty() || !player.isAlive()) {
            return;
        }
        BodyState body = BodyState.get(player);
        DeckHardware hw = DeckHardware.of(body);
        long now = player.level().getGameTime();
        for (DeckRuntime rt : List.copyOf(threads.values())) {
            if (rt.runner().state() != ProgramRunner.State.RUNNING) {
                continue;
            }
            while (!rt.pending.isEmpty() && rt.pending.peekFirst().dueTick() <= now) {
                rt.pending.pollFirst().action().run();
            }
            pollEvents(player, rt, body);

            // Computation costs Charge; a drained battery stalls the deck.
            boolean powered = !Body.chargeEnabled() || body.charge() > 0;
            int used = powered ? rt.runner().tick(hw.cyclesFor(rt.thread())) : 0;
            rt.lastCycles = used;
            if (Body.chargeEnabled() && used > 0) {
                rt.cycleDebt += used;
                while (rt.cycleDebt >= CYCLES_PER_CHARGE) {
                    rt.cycleDebt -= CYCLES_PER_CHARGE;
                    Body.spend(player, 1.0);
                }
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
        if (runner.subscribes("rotational_speed_changed")) {
            for (String name : player.getData(ModAttachments.DECK_DATA).ports().keySet()) {
                PortHandle port = new PortHandle(player, name);
                if (port.get("speed") instanceof Double speed) {
                    Double previous = rt.lastSpeeds.put(name, speed);
                    if (previous != null && Math.abs(previous - speed) > 1.0E-3) {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("source", port);
                        payload.put("old_speed", previous);
                        payload.put("rotational_speed", speed);
                        runner.fire("rotational_speed_changed", null, payload);
                    }
                } else {
                    rt.lastSpeeds.remove(name);
                }
            }
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
        int keys = DeckHardware.of(BodyState.get(player)).macroKeys();
        if (key >= 1 && key <= keys) {
            for (DeckRuntime rt : runtimes(player)) {
                rt.runner().fire("key", (double) key, Map.of("n", (double) key));
            }
        }
    }

    /** Records the outcome of a scripted action so programs can read {@code last_error}. */
    public static void recordResult(ServerPlayer player, UseResult result) {
        if (!result.ok()) {
            runtimes(player).forEach(rt -> rt.lastError = result.name());
        }
    }

    // ---------------------------------------------------------------- status for the screen

    public static void sendStatus(ServerPlayer player, String message) {
        BodyState body = BodyState.get(player);
        DeckHardware hw = DeckHardware.of(body);
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        List<DeckRuntime> running = runtimes(player);

        List<String> names = new ArrayList<>(data.programs().keySet());
        names.sort(String::compareToIgnoreCase);
        List<String> sources = new ArrayList<>();
        for (String name : names) {
            sources.add(data.programs().get(name));
        }
        String loaded = "";
        int footprint = 0;
        List<String> processes = new ArrayList<>();
        List<String> log = new ArrayList<>();
        boolean crashed = false;
        String crashReason = "";
        for (DeckRuntime rt : running) {
            ProgramRunner runner = rt.runner();
            if (rt.thread() == 0) {
                loaded = rt.programName();
            }
            footprint += runner.program().footprintCells();
            processes.add("T" + rt.thread() + " " + rt.programName() + " | " + runner.state().name().toLowerCase(java.util.Locale.ROOT)
                    + " | " + rt.lastCycles + "/" + hw.cyclesFor(rt.thread()) + " cycles | RAM " + runner.usedCells() + " | queued "
                    + runner.queued() + (runner.lastError().isEmpty() ? "" : " | " + runner.lastError()));
            for (String line : rt.logLines()) {
                log.add(running.size() > 1 ? "[T" + rt.thread() + "] " + line : line);
            }
            if (runner.state() == ProgramRunner.State.CRASHED) {
                crashed = true;
                crashReason = runner.lastError();
            }
        }
        String status = message;
        if (crashed) {
            status = (message.isEmpty() ? "" : message + " ") + "Program crashed: " + crashReason;
        }
        PacketDistributor.sendToPlayer(player, new DeckStatusPayload(hw.booted(), hw.isa().number(), hw.ramCells(),
                hw.storageBytes(), data.storedBytes(), names, sources, loaded, footprint, log, status,
                hw.threads(), hw.latencyTicks(), processes));
    }

    private DeckService() {
    }
}
