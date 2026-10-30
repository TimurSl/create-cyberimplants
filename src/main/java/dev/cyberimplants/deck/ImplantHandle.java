package dev.cyberimplants.deck;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.compiler.Ticks;
import dev.cyberimplants.synapse.vm.Handle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** A script's view of one implant: common members here, implant-specific ones in {@link ImplantProps}. */
final class ImplantHandle implements Handle {
    private final ServerPlayer player;
    private final ResourceLocation type;

    ImplantHandle(ServerPlayer player, ResourceLocation type) {
        this.player = player;
        this.type = type;
    }

    @Override
    public Object get(String member) {
        BodyState s = BodyState.get(player);
        int tier = s.tierOf(type);
        if (member.equals("installed")) {
            return tier > 0;
        }
        if (tier == 0) {
            return null; // absent hardware reads as NULL
        }
        ImplantAction action = ImplantActions.get(type);
        switch (member) {
            case "tier":
                return (double) tier;
            case "ready":
                return Body.ready(player, type) && (action == null || !action.isActive(player));
            case "active":
                return action == null ? ImplantProps.get(player, type, member) : action.isActive(player);
            case "cooldown":
                return new Ticks(s.cooldownRemaining(type));
            case "cost":
                return Stats.of(type, tier).chargeCost();
            default:
                return ImplantProps.get(player, type, member);
        }
    }

    @Override
    public boolean set(String member, Object value) {
        return BodyState.get(player).has(type) && ImplantProps.set(player, type, member, value);
    }

    /** Implant methods: the Combat Predictor's {@code predict.position} and {@code predict.windup}. */
    @Override
    public Object invoke(String method, List<Object> args, IsaLevel isa) {
        int tier = BodyState.get(player).tierOf(type);
        if (!type.equals(ImplantIds.COMBAT_PREDICTOR)) {
            throw new UnsupportedOperationException(method);
        }
        return tier == 0 ? null : Prediction.invoke(player, tier, method, args, isa);
    }
}
