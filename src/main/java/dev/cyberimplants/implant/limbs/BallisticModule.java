package dev.cyberimplants.implant.limbs;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ballistic Module: a forearm launcher firing standard flechettes. The hand must be empty. The magazine is persisted
 * in body data; reloading takes two seconds and draws Flechettes from the inventory. Input-bound: scripts can't fire.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class BallisticModule {
    public static final String MAGAZINE_KEY = "ballistic/magazine";
    private static final Map<UUID, Long> RELOAD_DONE = new ConcurrentHashMap<>();

    public static int magazine(Player player) {
        return (int) BodyState.get(player).number(MAGAZINE_KEY, 0);
    }

    public static boolean reloading(Player player) {
        Long done = RELOAD_DONE.get(player.getUUID());
        return done != null && player.level().getGameTime() < done;
    }

    /** Client-reported fire request; validated here. */
    public static void fire(ServerPlayer player) {
        BodyState body = BodyState.get(player);
        int tier = Math.max(Arms.moduleTier(body, Arms.Side.LEFT, ImplantIds.BALLISTIC_MODULE),
                Arms.moduleTier(body, Arms.Side.RIGHT, ImplantIds.BALLISTIC_MODULE));
        if (tier == 0 || Arms.freeHandWith(player, ImplantIds.BALLISTIC_MODULE) == null) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.BALLISTIC_MODULE, tier);
        if (reloading(player) || !Body.ready(player, ImplantIds.BALLISTIC_MODULE)) {
            return;
        }
        int magazine = magazine(player);
        if (magazine <= 0) {
            reload(player, (int) stats.value("magazine", 4), (long) stats.value("reload_ticks", 40));
            return;
        }
        if (!Body.spend(player, stats.chargeCost())) {
            player.displayClientMessage(Component.translatable("cyberimplants.use.no_charge"), true);
            return;
        }
        Body.update(player, s -> s.withData(MAGAZINE_KEY, magazine - 1));
        Body.startCooldown(player, ImplantIds.BALLISTIC_MODULE, stats.cooldownTicks());

        double velocity = stats.value("velocity", 2.0);
        Arrow flechette = new Arrow(player.level(), player, new ItemStack(Items.ARROW), null);
        flechette.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, (float) velocity, 0.5f);
        flechette.setBaseDamage(stats.value("damage", 3.0) / velocity);
        flechette.pickup = AbstractArrow.Pickup.DISALLOWED;
        player.level().addFreshEntity(flechette);
        ImplantFx.ballisticFire(player);
    }

    /** Explicit reload request (a full magazine reload from the inventory). */
    private static void reload(ServerPlayer player, int capacity, long ticks) {
        int taken = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && taken < capacity; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.FLECHETTE.get())) {
                int n = Math.min(stack.getCount(), capacity - taken);
                stack.shrink(n);
                taken += n;
            }
        }
        if (taken == 0) {
            player.displayClientMessage(Component.translatable("cyberimplants.ballistic.no_ammo"), true);
            return;
        }
        int loaded = taken;
        RELOAD_DONE.put(player.getUUID(), player.level().getGameTime() + ticks);
        Body.update(player, s -> s.withData(MAGAZINE_KEY, loaded));
        ImplantFx.ballisticReload(player);
        player.displayClientMessage(Component.translatable("cyberimplants.ballistic.reloading"), true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RELOAD_DONE.remove(event.getEntity().getUUID());
    }

    private BallisticModule() {
    }
}
