package dev.cyberimplants.content;

import dev.cyberimplants.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

/** Counts matching living entities inside a box around the sensor (docs/design/05-create-integration.md, section 5.1). */
public class ProximitySensorBlockEntity extends BlockEntity {
    public enum Filter {
        ANY(e -> true), HOSTILE(e -> e instanceof Enemy), PLAYER(e -> e instanceof Player), PASSIVE(e -> e instanceof Animal);

        private final Predicate<LivingEntity> test;

        Filter(Predicate<LivingEntity> test) {
            this.test = test;
        }

        public Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Half the box edge, so the box is up to 16 x 16 x 16. */
    public static final int MAX_HALF = 8;

    private int half = 4;
    private Filter filter = Filter.ANY;
    private int count;

    public ProximitySensorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROXIMITY_SENSOR.get(), pos, state);
    }

    public int count() {
        return count;
    }

    public int half() {
        return half;
    }

    public Filter filter() {
        return filter;
    }

    public void cycleFilter() {
        filter = filter.next();
        setChanged();
    }

    public void cycleSize() {
        half = half % MAX_HALF + 1;
        setChanged();
    }

    /** Called every few ticks on the server. */
    public void serverTick() {
        if (level == null || level.getGameTime() % 5 != 0) {
            return;
        }
        AABB box = new AABB(worldPosition).inflate(half - 0.5);
        int next = Math.min(15, level.getEntitiesOfClass(LivingEntity.class, box, filter.test::test).size());
        if (next != count) {
            count = next;
            setChanged();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Half", half);
        tag.putString("Filter", filter.name());
        tag.putInt("Count", count);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        half = Math.max(1, Math.min(MAX_HALF, tag.contains("Half") ? tag.getInt("Half") : 4));
        try {
            filter = Filter.valueOf(tag.getString("Filter"));
        } catch (IllegalArgumentException e) {
            filter = Filter.ANY;
        }
        count = tag.getInt("Count");
    }
}
