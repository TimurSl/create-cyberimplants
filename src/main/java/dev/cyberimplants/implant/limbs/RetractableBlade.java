package dev.cyberimplants.implant.limbs;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.PassiveModifiers;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Retractable Blade: deploy toggle. The module may sit in either arm's socket. While deployed and the main hand is empty,
 * PassiveModifiers turns the hand into a 6-damage weapon; the unarmed Power Actuators bonus applies on top because the
 * hand is still empty. Blades in both arms swing faster while both hands are empty. Tier V Thermal Edge (Layer 5)
 * sets the target on fire and ignores 40% of its armour.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class RetractableBlade {
    private static final float THERMAL_EDGE_FIRE_SECONDS = 4.0f;
    private static final float THERMAL_EDGE_ARMOR_KEPT = 0.6f;

    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            boolean next = !Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE);
            Toggles.set(player, ImplantIds.RETRACTABLE_BLADE, next);
            PassiveModifiers.apply(player);
            ImplantFx.blade(player, next);
            if (next) {
                double damage = Stats.of(ImplantIds.RETRACTABLE_BLADE, Arms.anyModuleTier(BodyState.get(player), ImplantIds.RETRACTABLE_BLADE))
                        .value("damage", 6.0);
                player.displayClientMessage(player.getMainHandItem().isEmpty()
                        ? Component.translatable("cyberimplants.blade.deployed", Math.round(damage))
                        : Component.translatable("cyberimplants.blade.hand_full"), true);
            } else {
                player.displayClientMessage(Component.translatable("cyberimplants.blade.retracted"), true);
            }
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE);
        }

        @Override
        public boolean stop(ServerPlayer player) {
            if (!isActive(player)) {
                return false;
            }
            Toggles.set(player, ImplantIds.RETRACTABLE_BLADE, false);
            PassiveModifiers.apply(player);
            return true;
        }
    };

    /** The tier of the blade the player strikes with right now: deployed, and the main hand is empty; 0 otherwise. */
    public static int strikingTier(Player player) {
        if (!player.getMainHandItem().isEmpty() || !Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE)) {
            return 0;
        }
        return Arms.anyModuleTier(BodyState.get(player), ImplantIds.RETRACTABLE_BLADE);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) {
            return;
        }
        int tier = strikingTier(player);
        if (tier > 0 && Stats.unlocked(ImplantIds.RETRACTABLE_BLADE, tier, "thermal_edge")) {
            event.getEntity().igniteForSeconds(THERMAL_EDGE_FIRE_SECONDS);
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * THERMAL_EDGE_ARMOR_KEPT);
        }
    }

    private RetractableBlade() {
    }
}
