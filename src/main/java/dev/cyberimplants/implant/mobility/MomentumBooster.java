package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;

/**
 * Momentum Booster: a 0-100 gauge filled by sprinting and spent whole as a dash plus a speed buff. Layer 1 has only
 * the sprint source; the gauge decays while the user is not sprinting.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class MomentumBooster {
    public static final String KEY = "momentum";

    public static double value(Player player) {
        return BodyState.get(player).number(KEY, 0);
    }

    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            double gauge = value(player);
            if (gauge < 5.0) {
                return UseResult.COOLDOWN;
            }
            TierStats stats = Stats.of(player, ImplantIds.MOMENTUM_BOOSTER);
            double fraction = gauge / 100.0;
            double blocks = stats.value("dash_blocks", 6) * fraction;
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0E-4) {
                return UseResult.DENIED;
            }
            flat = flat.normalize();
            // Ground friction makes an impulse v travel about 2.2 v; in the air the drag is lighter.
            double speed = player.onGround() ? blocks / 2.2 : blocks / 6.0;
            player.setDeltaMovement(flat.x * speed, Math.max(player.getDeltaMovement().y, 0.1), flat.z * speed);
            player.hurtMarked = true;
            int buffTicks = (int) (stats.value("buff_ticks", 40) * Math.max(fraction, 0.25));
            // Speed I is +20%: the table's +20/25/30/40/50% maps to Speed I, I, II, II, III.
            int amplifier = Math.max(0, (int) Math.round(stats.value("buff_speed", 0.20) / 0.2) - 1);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, buffTicks, amplifier));
            Body.update(player, s -> s.withData(KEY, 0));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8f, 1.2f);
            return UseResult.OK;
        }
    };

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.MOMENTUM_BOOSTER);
        if (tier == 0) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.MOMENTUM_BOOSTER, tier);
        double gauge = value(player);
        double next;
        if (player.isSprinting() && player.onGround()) {
            next = Math.min(100.0, gauge + stats.value("sprint_gain_per_second", 8.0) / 20.0);
        } else {
            next = Math.max(0.0, gauge - stats.value("decay_per_second", 5.0) / 20.0);
        }
        if (next != gauge) {
            Body.updateQuiet(player, s -> s.withData(KEY, next));
            if (gauge < 100.0 && next >= 100.0) {
                DeckEvents.fire(player, "momentum_full", Map.of());
            }
        }
    }

    private MomentumBooster() {
    }
}
