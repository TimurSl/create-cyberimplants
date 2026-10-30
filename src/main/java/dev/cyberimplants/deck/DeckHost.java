package dev.cyberimplants.deck;

import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.compat.create.CreateCompat;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.implant.optical.TargetTracking;
import dev.cyberimplants.registry.ModAttachments;
import dev.cyberimplants.synapse.vm.Host;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/** Binds a running Synapse program to its owner's implants. */
final class DeckHost implements Host {
    private final ServerPlayer player;
    private final java.util.function.Supplier<DeckRuntime> runtime;

    DeckHost(ServerPlayer player, java.util.function.Supplier<DeckRuntime> runtime) {
        this.player = player;
        this.runtime = runtime;
    }

    @Override
    public Object global(String name) {
        switch (name) {
            case "self":
                return new SelfHandle(player);
            case "deck":
                return new DeckStateHandle(player, runtime);
            case "last_error": {
                DeckRuntime r = runtime.get();
                return r == null || r.lastError.isEmpty() ? null : r.lastError;
            }
            default: {
                ResourceLocation type = DeckAliases.resolve(name);
                return type == null ? null : new ImplantHandle(player, type);
            }
        }
    }

    @Override
    public Object device(String kind, String label) {
        return kind.equals("port") ? new PortHandle(player, label) : null;
    }

    /**
     * Fires the implant after the chassis' neural latency (4 ticks at Tier I down to 0 at Tier V). With latency the
     * script sees an optimistic TRUE; a refusal when the action finally runs lands in {@code last_error}.
     */
    @Override
    public boolean activate(String implant, Object at, Map<String, Object> with) {
        ResourceLocation type = DeckAliases.resolve(implant);
        if (type == null) {
            return fail(UseResult.MISSING);
        }
        int latency = DeckHardware.of(dev.cyberimplants.body.BodyState.get(player)).latencyTicks();
        DeckRuntime r = runtime.get();
        if (latency > 0 && r != null && dev.cyberimplants.config.Layers.atLeast(2)) {
            r.pending.add(new DeckRuntime.Pending(player.level().getGameTime() + latency, () -> {
                UseResult result = ImplantActions.use(player, type, UseContext.scripted(at, with));
                if (!result.ok()) {
                    fail(result);
                }
            }));
            return true;
        }
        UseResult result = ImplantActions.use(player, type, UseContext.scripted(at, with));
        return result.ok() || fail(result);
    }

    @Override
    public boolean configure(String implant, String key, Object value) {
        ResourceLocation type = DeckAliases.resolve(implant);
        if (type == null) {
            return fail(UseResult.MISSING);
        }
        return ImplantProps.set(player, type, key, value) || fail(UseResult.DENIED);
    }

    @Override
    public boolean manipulate(Object target, String action, Object argument) {
        if (!dev.cyberimplants.config.Layers.atLeast(2)) {
            return fail(UseResult.DENIED);
        }
        net.minecraft.core.BlockPos pos = null;
        if (target instanceof PortHandle port) {
            pos = port.targetPos();
        } else if (target instanceof dev.cyberimplants.synapse.vm.Position p) {
            pos = net.minecraft.core.BlockPos.containing(p.x(), p.y(), p.z());
        }
        if (pos == null) {
            return fail(UseResult.MISSING);
        }
        UseResult result = dev.cyberimplants.implant.limbs.RemoteManipulator.manipulate(player, pos, action);
        return result.ok() || fail(result);
    }

    @Override
    public boolean deactivate(String implant) {
        ResourceLocation type = DeckAliases.resolve(implant);
        ImplantAction action = type == null ? null : ImplantActions.get(type);
        if (action == null) {
            return fail(UseResult.MISSING);
        }
        return action.stop(player) || fail(UseResult.DENIED);
    }

    @Override
    public boolean toggle(String implant) {
        return activate(implant, null, Map.of());
    }

    @Override
    public boolean scan() {
        return activate("scanner", null, Map.of());
    }

    @Override
    public boolean overclock() {
        UseResult result = DeckService.overclock(player);
        return result.ok() || fail(result);
    }

    /** Machine Interface Tier V: {@code network} names a bound Neural Port plugged into a Stock Link or Stock Ticker. */
    @Override
    public boolean request(String item, int count, String network, String address) {
        if (!logistics()) {
            return fail(UseResult.DENIED);
        }
        ItemStack stack = item(item);
        Optional<UUID> freq = network(network);
        if (stack.isEmpty() || freq.isEmpty()) {
            return fail(UseResult.MISSING);
        }
        return CreateCompat.logistics().request(freq.get(), stack, count, address) || fail(UseResult.MISSING);
    }

    @Override
    public Double stock(String network, String item) {
        if (!logistics()) {
            fail(UseResult.DENIED);
            return null;
        }
        ItemStack stack = item(item);
        Optional<UUID> freq = network(network);
        if (stack.isEmpty() || freq.isEmpty()) {
            return null;
        }
        OptionalInt count = CreateCompat.logistics().stock(freq.get(), stack);
        return count.isPresent() ? (double) count.getAsInt() : null;
    }

    private boolean logistics() {
        return Layers.atLeast(5) && DeckHardware.of(BodyState.get(player)).machineInterface() >= 5;
    }

    private Optional<UUID> network(String portName) {
        BlockPos target = new PortHandle(player, portName).targetPos();
        return target == null ? Optional.empty() : CreateCompat.logistics().network(player, target);
    }

    private static ItemStack item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    public Map<String, Object> restorePersisted() {
        DeckRuntime r = runtime.get();
        Map<String, String> saved = r == null ? null : player.getData(ModAttachments.DECK_DATA).persisted().get(r.programName());
        if (saved == null) {
            return Map.of();
        }
        Map<String, Object> out = new HashMap<>();
        saved.forEach((name, text) -> out.put(name, DeckData.decode(text)));
        return out;
    }

    @Override
    public void persist(Map<String, Object> values) {
        DeckRuntime r = runtime.get();
        if (r == null) {
            return;
        }
        Map<String, String> encoded = new HashMap<>();
        values.forEach((name, value) -> encoded.put(name, DeckData.encode(value)));
        player.setData(ModAttachments.DECK_DATA, player.getData(ModAttachments.DECK_DATA).withPersisted(r.programName(), encoded));
    }

    @Override
    public boolean tag(Object entity) {
        if (entity instanceof EntityHandle h) {
            Entity e = h.resolve();
            if (e != null) {
                UseResult result = TargetTracking.tag(player, e, false);
                return result.ok() || fail(result);
            }
        }
        return fail(UseResult.MISSING);
    }

    @Override
    public boolean untag(Object entity) {
        return TargetTracking.untag(player) || fail(UseResult.MISSING);
    }

    @Override
    public void log(String line) {
        DeckRuntime r = runtime.get();
        if (r != null) {
            r.log(line);
        }
    }

    @Override
    public void alert(String message) {
        player.displayClientMessage(Component.literal("[Deck] " + message), true);
    }

    @Override
    public long now() {
        DeckRuntime r = runtime.get();
        return r == null ? 0 : player.level().getGameTime() - r.bootTick;
    }

    @Override
    public double random() {
        DeckRuntime r = runtime.get();
        return r == null ? 0 : r.nextRandom();
    }

    private boolean fail(UseResult result) {
        DeckService.recordResult(player, result);
        return false;
    }

    /** Unused marker so the tracker id stays referenced from one place. */
    static ResourceLocation trackerType() {
        return ImplantIds.TARGET_TRACKING;
    }
}
