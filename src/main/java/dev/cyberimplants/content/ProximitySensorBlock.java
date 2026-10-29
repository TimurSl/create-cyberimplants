package dev.cyberimplants.content;

import com.mojang.serialization.MapCodec;
import dev.cyberimplants.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Proximity Sensor: emits redstone equal to the number of matching living entities in its box (capped at 15).
 * Right-click cycles the filter, sneak-right-click the box size. Binding it to a deck needs the Remote Sensor
 * Uplink (a later layer).
 */
public class ProximitySensorBlock extends BaseEntityBlock {
    public static final MapCodec<ProximitySensorBlock> CODEC = simpleCodec(ProximitySensorBlock::new);

    public ProximitySensorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProximitySensorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.PROXIMITY_SENSOR.get()) {
            return null;
        }
        return (l, p, s, be) -> ((ProximitySensorBlockEntity) be).serverTick();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ProximitySensorBlockEntity sensor ? sensor.count() : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ProximitySensorBlockEntity sensor ? sensor.count() : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ProximitySensorBlockEntity sensor) {
            if (player.isShiftKeyDown()) {
                sensor.cycleSize();
                player.displayClientMessage(Component.translatable("cyberimplants.sensor.size", sensor.half() * 2), true);
            } else {
                sensor.cycleFilter();
                player.displayClientMessage(Component.translatable("cyberimplants.sensor.filter", sensor.filter().name().toLowerCase()), true);
            }
        }
        return InteractionResult.CONSUME;
    }
}
