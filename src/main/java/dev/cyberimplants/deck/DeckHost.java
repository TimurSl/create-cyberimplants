package dev.cyberimplants.deck;

import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.implant.optical.TargetTracking;
import dev.cyberimplants.synapse.vm.Host;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Map;

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
    public boolean activate(String implant, Object at, Map<String, Object> with) {
        ResourceLocation type = DeckAliases.resolve(implant);
        if (type == null) {
            return fail(UseResult.MISSING);
        }
        UseResult result = ImplantActions.use(player, type, UseContext.scripted(at, with));
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
