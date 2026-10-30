package dev.cyberimplants.compat.create.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.cyberimplants.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Precision Etcher: mounted like a Mechanical Press, one block above a belt or depot, with its shaft running
 * across the facing axis. Its emitter head reaches down into the gap.
 */
public class EtcherBlock extends HorizontalKineticBlock implements IBE<EtcherBlockEntity> {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 6, 0, 16, 16, 16), Block.box(4, 0, 4, 12, 6, 12));

    public static final MapCodec<EtcherBlock> CODEC = simpleCodec(EtcherBlock::new);

    public EtcherBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Class<EtcherBlockEntity> getBlockEntityClass() {
        return EtcherBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EtcherBlockEntity> getBlockEntityType() {
        return ModBlockEntities.ETCHER.get();
    }
}
