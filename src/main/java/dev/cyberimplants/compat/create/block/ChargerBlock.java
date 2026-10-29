package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Kinetic Charger: a floor pad that turns shaft rotation into body Charge. */
public class ChargerBlock extends RotatedPillarKineticBlock implements IBE<ChargerBlockEntity> {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public ChargerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Class<ChargerBlockEntity> getBlockEntityClass() {
        return ChargerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ChargerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.CHARGER.get();
    }
}
