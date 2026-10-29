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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Implantation Rig: a kinetic surgical stool. Right-click while it spins to operate on yourself; for the length of an
 * operation the Rig straps you onto its cushion. The shaft goes into the gearbox at the back.
 */
public class RigBlock extends HorizontalKineticBlock implements IBE<RigBlockEntity> {
    public static final MapCodec<RigBlock> CODEC = simpleCodec(RigBlock::new);

    /** The stool facing north (gearbox to the south), in model pixels {x1, y1, z1, x2, y2, z2}. */
    private static final double[][] NORTH_BOXES = {
            {1, 0, 1, 15, 2, 15},     // base plate
            {5, 2, 5, 11, 10, 11},    // piston column
            {4, 2, 11, 12, 10, 16},   // gearbox
            {1, 10, 1, 15, 13, 15},   // seat pan and cushion
            {0, 12, 4, 2, 15, 12},    // left restraint
            {14, 12, 4, 16, 15, 12},  // right restraint
            {3, 13, 13, 13, 15, 15},  // back bolster
    };
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : NORTH_BOXES) {
                shape = Shapes.or(shape, rotated(b, facing));
            }
            SHAPES.put(facing, shape.optimize());
        }
    }

    /** Rotates a north-facing box the way the blockstate rotates the model (y = 90 per step clockwise). */
    private static VoxelShape rotated(double[] b, Direction facing) {
        double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
        return switch (facing) {
            case EAST -> Block.box(16 - z2, b[1], x1, 16 - z1, b[4], x2);
            case SOUTH -> Block.box(16 - x2, b[1], 16 - z2, 16 - x1, b[4], 16 - z1);
            case WEST -> Block.box(z1, b[1], 16 - x2, z2, b[4], 16 - x1);
            default -> Block.box(x1, b[1], z1, x2, b[4], z2);
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(HORIZONTAL_FACING), Shapes.block());
    }

    public RigBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
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
