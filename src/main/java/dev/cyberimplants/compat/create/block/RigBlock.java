package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.cyberimplants.network.OpenRigPayload;
import dev.cyberimplants.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Implantation Rig: a kinetic surgical chair. Right-click while it spins to operate on yourself. */
public class RigBlock extends HorizontalKineticBlock implements IBE<RigBlockEntity> {
    public RigBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (!RigBlockEntity.canOperate(serverPlayer, pos)) {
                serverPlayer.displayClientMessage(Component.translatable("cyberimplants.rig.no_power"), true);
                return InteractionResult.CONSUME;
            }
            PacketDistributor.sendToPlayer(serverPlayer, new OpenRigPayload(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Class<RigBlockEntity> getBlockEntityClass() {
        return RigBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RigBlockEntity> getBlockEntityType() {
        return ModBlockEntities.RIG.get();
    }
}
