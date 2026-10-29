package dev.cyberimplants.content;

import com.mojang.serialization.MapCodec;
import dev.cyberimplants.deck.DeckBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Neural Port: a small data plug placed against a face of a machine. {@code FACING} points at the machine. Its owner
 * right-clicks it to bind it to their deck; the port's name is the placing item's custom name (rename it in an anvil)
 * or "Port x y z". Layer 1 exposes the target's rotation speed; with a Machine Interface Tier II the deck can also drive the port's redstone output.
 */
public class NeuralPortBlock extends Block implements EntityBlock {
    public static final MapCodec<NeuralPortBlock> CODEC = simpleCodec(NeuralPortBlock::new);
    public static final DirectionProperty FACING = DirectionProperty.create("facing");

    private static final VoxelShape DOWN = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape UP = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape NORTH = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape WEST = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape EAST = Block.box(11, 5, 5, 16, 11, 11);

    public NeuralPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The port sits on the clicked face and looks at the block behind it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    /** The block the port reads from. */
    public static BlockPos target(BlockPos portPos, BlockState state) {
        return portPos.relative(state.getValue(FACING));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof NeuralPortBlockEntity port) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            String name = custom != null ? custom.getString() : "Port " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
            port.setOwner(player.getUUID(), name);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof NeuralPortBlockEntity port) {
            if (port.owner() != null && !port.owner().equals(player.getUUID())) {
                serverPlayer.displayClientMessage(Component.translatable("cyberimplants.port.not_owner"), true);
            } else {
                DeckBindings.togglePort(serverPlayer, GlobalPos.of(level.dimension(), pos), port.portName());
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** Emits from every face except the attached one; {@code direction} points from the neighbour toward this block. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == state.getValue(FACING) || !(level.getBlockEntity(pos) instanceof NeuralPortBlockEntity port)) {
            return 0;
        }
        return port.redstoneOut();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NeuralPortBlockEntity(pos, state);
    }
}
