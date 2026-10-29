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
