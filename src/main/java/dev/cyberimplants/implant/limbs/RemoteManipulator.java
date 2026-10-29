package dev.cyberimplants.implant.limbs;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Remote Manipulator: uses simple blocks (levers, buttons, doors, trapdoors, fence gates) at a distance. The use goes
 * through the normal interaction path, so protection mods see a real player interaction.
 */
public final class RemoteManipulator {
    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            BodyState body = BodyState.get(player);
            if (Arms.freeHandWith(player, ImplantIds.REMOTE_MANIPULATOR) == null
                    && !(Arms.hasModule(body, Arms.Side.LEFT, ImplantIds.REMOTE_MANIPULATOR)
                    || Arms.hasModule(body, Arms.Side.RIGHT, ImplantIds.REMOTE_MANIPULATOR))) {
                return UseResult.MISSING;
            }
            TierStats stats = Stats.of(player, ImplantIds.REMOTE_MANIPULATOR);
            if (!Body.ready(player, ImplantIds.REMOTE_MANIPULATOR)) {
                return UseResult.COOLDOWN;
            }
            double range = stats.value("range", 6);
            HitResult hit = player.pick(range, 1.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
                return UseResult.NO_LOS;
            }
            BlockPos pos = blockHit.getBlockPos();
            ServerLevel level = player.serverLevel();
            Block block = level.getBlockState(pos).getBlock();
            if (!isSimple(block)) {
                return UseResult.DENIED;
            }
            if (!Body.spend(player, stats.chargeCost())) {
                return UseResult.NO_CHARGE;
            }
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, blockHit);
            Body.startCooldown(player, ImplantIds.REMOTE_MANIPULATOR, stats.cooldownTicks());
            tether(level, player.getEyePosition().add(0, -0.3, 0), Vec3.atCenterOf(pos));
            return UseResult.OK;
        }
    };

    /**
     * A scripted {@code MANIPULATE}: USE a simple block, or PULL item entities and XP orbs to the user (Tier II). Range,
     * line of sight, Charge and cooldown are the same as for a manual use. WRENCH and SET_VALUE need Tier III (Layer 3).
     */
    public static UseResult manipulate(ServerPlayer player, BlockPos pos, String action) {
        TierStats stats = Stats.of(player, ImplantIds.REMOTE_MANIPULATOR);
        int tier = BodyState.get(player).tierOf(ImplantIds.REMOTE_MANIPULATOR);
        if (tier == 0) {
            return UseResult.MISSING;
        }
        if (!Body.ready(player, ImplantIds.REMOTE_MANIPULATOR)) {
            return UseResult.COOLDOWN;
        }
        Vec3 target = Vec3.atCenterOf(pos);
        if (player.getEyePosition().distanceTo(target) > stats.value("range", 6)) {
            return UseResult.RANGE;
        }
        ServerLevel level = player.serverLevel();
        BlockHitResult clip = level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(), target,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (clip.getType() == HitResult.Type.BLOCK && !clip.getBlockPos().equals(pos)) {
            return UseResult.NO_LOS;
        }
        switch (action) {
            case "USE" -> {
                if (!isSimple(level.getBlockState(pos).getBlock())) {
                    return UseResult.DENIED;
                }
                if (!Body.spend(player, stats.chargeCost())) {
                    return UseResult.NO_CHARGE;
                }
                player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                        new BlockHitResult(target, net.minecraft.core.Direction.UP, pos, false));
            }
            case "PULL" -> {
                if (tier < 2) {
                    return UseResult.DENIED;
                }
                if (!Body.spend(player, stats.chargeCost())) {
                    return UseResult.NO_CHARGE;
                }
                var pulled = level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(2.0),
                        e -> e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb);
                for (var e : pulled) {
                    e.setDeltaMovement(player.position().add(0, 0.5, 0).subtract(e.position()).scale(0.25));
                    e.hurtMarked = true;
                }
            }
            default -> {
                return UseResult.DENIED;
            }
        }
        Body.startCooldown(player, ImplantIds.REMOTE_MANIPULATOR, stats.cooldownTicks());
        tether(level, player.getEyePosition().add(0, -0.3, 0), target);
        return UseResult.OK;
    }

    private static boolean isSimple(Block block) {
        return block instanceof LeverBlock || block instanceof ButtonBlock || block instanceof DoorBlock
                || block instanceof TrapDoorBlock || block instanceof FenceGateBlock;
    }

    /** The tether is visible and can be followed back to the user. */
    private static void tether(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = (int) Math.max(2, delta.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale((double) i / steps));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private RemoteManipulator() {
    }
}
