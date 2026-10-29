package dev.cyberimplants.content;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.registry.ModBlocks;
import dev.cyberimplants.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * The invisible seat of the Implantation Rig. The Rig sits its patient on this entity for the length of an operation;
 * while the seat is {@linkplain #locked() locked} the patient cannot stand up (the mount event is cancelled and the
 * client drops movement input, see {@code ClientRig}). The seat removes itself as soon as it is empty or the Rig is gone,
 * and it is never saved, so a crash or a logout cannot leave anyone strapped in.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public class RigSeatEntity extends Entity {
    /** Height of the stool's cushion above the Rig block's floor, in blocks. */
    public static final double SEAT_HEIGHT = 13.0 / 16.0;

    private static final EntityDataAccessor<Boolean> LOCKED = SynchedEntityData.defineId(RigSeatEntity.class, EntityDataSerializers.BOOLEAN);

    public RigSeatEntity(EntityType<? extends RigSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A locked seat on top of the Rig at {@code rig}, facing {@code yaw}. The caller adds it to the level. */
    public static RigSeatEntity create(Level level, BlockPos rig, float yaw) {
        RigSeatEntity seat = new RigSeatEntity(ModEntities.RIG_SEAT.get(), level);
        seat.moveTo(rig.getX() + 0.5, rig.getY() + SEAT_HEIGHT, rig.getZ() + 0.5, yaw, 0);
        seat.setLocked(true);
        return seat;
    }

    public boolean locked() {
        return entityData.get(LOCKED);
    }

    public void setLocked(boolean locked) {
        entityData.set(LOCKED, locked);
    }

    /** The Rig block under the seat. */
    public BlockPos rigPos() {
        return BlockPos.containing(getX(), getY() - SEAT_HEIGHT + 0.01, getZ());
    }

    /** Unlocks, lets the patient stand up on top of the stool and removes the seat. */
    public void release() {
        setLocked(false);
        ejectPassengers();
        discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LOCKED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !level().getBlockState(rigPos()).is(ModBlocks.IMPLANTATION_RIG.get()))) {
            release();
        }
    }

    /** The patient's hips rest on the cushion: a player's vehicle attachment sits at their hips. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(getX(), getY() + 0.25, getZ());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** True when {@code player} is strapped into a locked Rig seat. Works on both sides. */
    public static boolean restrained(Player player) {
        return player.getVehicle() instanceof RigSeatEntity seat && seat.locked();
    }

    /**
     * Patients cannot stand up mid-operation. Dying and disconnecting still dismount, so nobody stays stuck. Only the
     * server refuses: the client always follows the server's passenger updates (its lock flag can lag behind them).
     */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide
                && event.getEntityBeingMounted() instanceof RigSeatEntity seat && seat.locked()
                && event.getEntityMounting() instanceof LivingEntity patient && patient.isAlive()
                && !(patient instanceof ServerPlayer sp && sp.hasDisconnected())) {
            event.setCanceled(true);
        }
    }
}
