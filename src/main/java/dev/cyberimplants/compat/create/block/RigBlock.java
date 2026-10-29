package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.cyberimplants.network.OpenRigPayload;
import dev.cyberimplants.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Implantation Rig: a kinetic surgical chair. Right-click while it spins to operate on yourself. */
public class RigBlock extends HorizontalKineticBlock implements IBE<RigBlockEntity> {
    public static final MapCodec<RigBlock> CODEC = simpleCodec(RigBlock::new);

    public RigBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.Block> codec() {
        return CODEC;
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

    /** Buckets and other fluid containers fill the Rig's gel tank. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Operation progress for comparators. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RigBlockEntity rig ? rig.progress() : 0;
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
