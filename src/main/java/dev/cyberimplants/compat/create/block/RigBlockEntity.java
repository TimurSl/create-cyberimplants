package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.cyberimplants.body.BodyOps;
import dev.cyberimplants.body.BodySync;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.content.RigSeatEntity;
import dev.cyberimplants.implant.ImplantItem;
import dev.cyberimplants.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.UUID;

/**
 * Kinetic block entity of the Implantation Rig (docs/design/01-core-systems.md, section 1.5). Layer 2 operations are
 * timed (5/8/12/16/20 s by implant tier), need a minimum shaft speed (16/32/64/128/256 RPM) and 250 mB of gel (Bio-Interface
 * Gel for Tiers I-III, Neural Gel for IV-V), and abort when the Rig slows or the gel runs out. For the length of an
 * operation the patient is strapped onto the stool (a locked {@link RigSeatEntity}); leaving it any other way (an ender
 * pearl, death) aborts. Under {@code layers.maxEnabled = 1} operations are instant and any rotation will do.
 *
 * <p>The running operation, the gel and the speed are synced to clients so the screen, the HUD and the renderer can
 * show them ({@link #view()}).
 */
public class RigBlockEntity extends KineticBlockEntity {
    /** Impact in SU per RPM (docs/design/05-create-integration.md, section 5.1). */
    public static final float STRESS_IMPACT = 8.0f;
    public static final double MAX_USE_DISTANCE = 6.0;
    public static final int[] MIN_RPM = {16, 32, 64, 128, 256};
    public static final int[] SECONDS = {5, 8, 12, 16, 20};
    public static final int GEL_PER_OPERATION = 250;
    public static final int TANK_CAPACITY = 1000;
    public static final float ABORT_DAMAGE = 4.0f;
    /** How often a running operation re-syncs its progress to clients; they count the ticks in between themselves. */
    private static final int SYNC_INTERVAL = 10;

    private record Operation(UUID patient, boolean install, SlotId slot, int tier, int inventorySlot, ItemStack expected,
                             ResourceLocation type, int total) {
    }

    private final FluidTank tank = new FluidTank(TANK_CAPACITY,
            fluid -> fluid.is(ModFluids.BIO_GEL.get()) || fluid.is(ModFluids.NEURAL_GEL.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncPending = true;
        }
    };

    private Operation operation;
    private RigSeatEntity seat;
    private int elapsed;
    private int gelPaid;
    private boolean syncPending;
    /** Client side: the operation as last synced, with {@code elapsed} counted on locally. */
    private RigView.Operation clientOperation;

    public RigBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }

    public IFluidHandler tank() {
        return tank;
    }

    /** Any rotation at all opens the Rig's screen; the per-tier RPM is checked when an operation starts and while it runs. */
    public boolean operational() {
        return Math.abs(getSpeed()) > 0f && !isOverStressed();
    }

    public boolean rpmAtLeast(int tier) {
        return Layers.atLeast(2) ? Math.abs(getSpeed()) >= MIN_RPM[tier - 1] && !isOverStressed() : operational();
    }

    /** Comparator output: operation progress 0-15. */
    public int progress() {
        return operation == null ? 0 : Math.min(15, 15 * elapsed / Math.max(1, operation.total()));
    }

    public boolean busy() {
        return operation != null;
    }

    /** A snapshot for screens and renderers. On the client the operation is the synced one. */
    public RigView view() {
        RigView.Operation op = clientOperation;
        if (level != null && !level.isClientSide && operation != null) {
            op = new RigView.Operation(operation.patient(), operation.install(), operation.slot(), operation.tier(),
                    operation.type(), elapsed, operation.total());
        }
        boolean locked = level != null && level.hasNeighborSignal(worldPosition);
        return new RigView(Math.abs(getSpeed()), isOverStressed(), locked, tank.getFluidAmount(),
                tank.getFluid().is(ModFluids.NEURAL_GEL.get()), op);
    }

    /** True when the player is close enough to this Rig and it is running. */
    public static boolean canOperate(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_USE_DISTANCE * MAX_USE_DISTANCE) {
            return false;
        }
        return player.level().getBlockEntity(pos) instanceof RigBlockEntity rig && rig.operational();
    }

    /** Whether operations on this tier need Neural Gel (Tiers IV and V) rather than Bio-Interface Gel. */
    public static boolean needsNeuralGel(int tier) {
        return tier > 3;
    }

    /** Operation time in ticks for an implant tier, including the server's time multiplier. */
    public static int operationTicks(int tier) {
        double multiplier;
        try {
            multiplier = ServerConfig.RIG_TIME_MULTIPLIER.get();
        } catch (IllegalStateException e) {
            multiplier = 1.0; // a client without the server config
        }
        return Math.max(1, (int) Math.round(SECONDS[tier - 1] * 20 * multiplier));
    }

    private static FluidStack gelFor(int tier) {
        return new FluidStack(needsNeuralGel(tier) ? ModFluids.NEURAL_GEL.get() : ModFluids.BIO_GEL.get(), GEL_PER_OPERATION);
    }

    private static Component gelName(int tier) {
        return Component.translatable(needsNeuralGel(tier) ? "fluid_type.cyberimplants.neural_gel" : "fluid_type.cyberimplants.bio_interface_gel");
    }

    // ---------------------------------------------------------------- starting an operation

    /**
     * Starts installing the implant from {@code inventorySlot} into {@code slot}, or removing the one in {@code slot}.
     *
     * @return the reason it could not start, or null
     */
    public Component begin(ServerPlayer player, boolean install, SlotId slot, int inventorySlot) {
        if (operation != null) {
            return Component.translatable("cyberimplants.rig.busy");
        }
        if (level != null && level.hasNeighborSignal(worldPosition)) {
            return Component.translatable("cyberimplants.rig.locked");
        }
        BodyState body = BodyState.get(player);
        ItemStack expected = ItemStack.EMPTY;
        ResourceLocation type;
        int tier;
        if (install) {
            if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) {
                return Component.translatable("cyberimplants.rig.unknown_implant");
            }
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            if (!(stack.getItem() instanceof ImplantItem item)) {
                return Component.translatable("cyberimplants.rig.unknown_implant");
            }
            tier = ImplantItem.tierOf(stack);
            BodyOps.Result r = BodyOps.canInstall(body, item.typeId(), tier, slot);
            if (!r.ok()) {
                return Component.translatable(r.translationKey());
            }
            expected = stack.copyWithCount(1);
            type = item.typeId();
        } else {
            InstalledImplant installed = body.installed().get(slot);
            BodyOps.Result r = BodyOps.canRemove(body, slot);
            if (installed == null || !r.ok()) {
                return Component.translatable(r.translationKey());
            }
            tier = installed.tier();
            type = installed.type();
        }

        if (!Layers.atLeast(2)) {
            BodyOps.Result r = install ? BodyOps.install(player, slot, player.getInventory().getItem(inventorySlot)) : BodyOps.remove(player, slot);
            return r.ok() ? null : Component.translatable(r.translationKey());
        }
        if (!rpmAtLeast(tier)) {
            return Component.translatable("cyberimplants.rig.low_rpm", MIN_RPM[tier - 1]);
        }
        if (!(tank.getFluidAmount() >= GEL_PER_OPERATION && tank.getFluid().is(gelFor(tier).getFluid()))) {
            return Component.translatable("cyberimplants.rig.no_gel", GEL_PER_OPERATION, gelName(tier));
        }
        if (!strapIn(player)) {
            return Component.translatable("cyberimplants.rig.cannot_seat");
        }
        operation = new Operation(player.getUUID(), install, slot, tier, inventorySlot, expected, type, operationTicks(tier));
        elapsed = 0;
        gelPaid = 0;
        player.level().playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.7f, 1.4f);
        player.level().playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
        setChanged();
        sendData();
        return null;
    }

    /** Sits the patient on the stool, facing the Rig's front, and locks the seat. */
    private boolean strapIn(ServerPlayer player) {
        releaseSeat();
        float yaw = getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING).toYRot();
        RigSeatEntity next = RigSeatEntity.create(player.level(), worldPosition, yaw);
        if (!player.level().addFreshEntity(next)) {
            return false;
        }
        if (!player.startRiding(next, true)) {
            next.discard();
            return false;
        }
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        seat = next;
        return true;
    }

    private void releaseSeat() {
        if (seat != null) {
            seat.release();
            seat = null;
        }
    }

    // ---------------------------------------------------------------- running it

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            RigView.Operation op = clientOperation;
            if (op != null && op.elapsed() < op.total()) {
                clientOperation = new RigView.Operation(op.patient(), op.install(), op.slot(), op.tier(), op.type(), op.elapsed() + 1, op.total());
            }
            return;
        }
        if (syncPending && (operation == null || level.getGameTime() % SYNC_INTERVAL == 0)) {
            syncPending = false;
            sendData();
        }
        if (operation == null) {
            return;
        }
        Operation op = operation;
        Player found = level.getPlayerByUUID(op.patient());
        if (!(found instanceof ServerPlayer player) || !player.isAlive()) {
            finish(); // the patient is gone: nothing to hurt, nothing was changed
            return;
        }
        if (seat == null || player.getVehicle() != seat) {
            abort(player, "moved", true);
            return;
        }
        if (!rpmAtLeast(op.tier())) {
            abort(player, "rpm", true);
            return;
        }
        if (!op.install() && !BodyState.get(player).installed().containsKey(op.slot())) {
            abort(player, "invalid", false);
            return;
        }
        // Gel drains evenly over the operation.
        int due = GEL_PER_OPERATION * (elapsed + 1) / op.total();
        if (due > gelPaid) {
            FluidStack gel = gelFor(op.tier());
            FluidStack drained = tank.drain(new FluidStack(gel.getFluid(), due - gelPaid), IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() < due - gelPaid) {
                abort(player, "gel", true);
                return;
            }
            gelPaid = due;
        }
        elapsed++;
        effects((ServerLevel) level, op);
        if (elapsed % SYNC_INTERVAL == 0) {
            sendData();
        }
        if (elapsed >= op.total()) {
            complete(player, op);
        }
    }

    /** Sparks from the surgical arms and a low hum while the Rig works. */
    private void effects(ServerLevel server, Operation op) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + RigSeatEntity.SEAT_HEIGHT;
        double z = worldPosition.getZ() + 0.5;
        if (elapsed % 4 == 0) {
            double sweep = 0.3 + 1.4 * (0.5 + 0.5 * Math.sin(elapsed * 0.15));
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + sweep, z, 3, 0.3, 0.05, 0.3, 0.02);
        }
        if (elapsed % 40 == 0) {
            server.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.8f);
        }
        if (elapsed == op.total() - 1) {
            server.sendParticles(ParticleTypes.END_ROD, x, y + 0.9, z, 24, 0.35, 0.6, 0.35, 0.03);
        }
    }

    private void complete(ServerPlayer player, Operation op) {
        BodyOps.Result result;
        if (op.install()) {
            ItemStack stack = player.getInventory().getItem(op.inventorySlot());
            if (!ItemStack.isSameItemSameComponents(stack, op.expected())) {
                abort(player, "invalid", false);
                return;
            }
            result = BodyOps.install(player, op.slot(), stack);
        } else {
            result = BodyOps.remove(player, op.slot());
        }
        if (result.ok()) {
            player.displayClientMessage(Component.translatable("cyberimplants.rig.done"), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 1.0f, 0.8f);
            player.level().playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
        } else {
            player.displayClientMessage(Component.translatable(result.translationKey()), true);
        }
        BodySync.markDirty(player);
        finish();
    }

    /** The patient takes the abort damage; the implant never left their inventory (or their body). */
    private void abort(ServerPlayer player, String reason, boolean hurt) {
        player.displayClientMessage(Component.translatable("cyberimplants.rig.abort." + reason), true);
        player.level().playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8f, 0.6f);
        finish();
        if (hurt) {
            player.hurt(player.damageSources().generic(), ABORT_DAMAGE);
        }
    }

    private void finish() {
        operation = null;
        elapsed = 0;
        gelPaid = 0;
        releaseSeat();
        setChanged();
        sendData();
    }

    /** Breaking the Rig frees whoever is strapped in. */
    @Override
    public void remove() {
        releaseSeat();
        super.remove();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 2.5, 0).inflate(0.5);
    }

    // ---------------------------------------------------------------- persistence and sync

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Gel", tank.writeToNBT(registries, new CompoundTag()));
        if (clientPacket && operation != null) {
            CompoundTag op = new CompoundTag();
            op.putUUID("Patient", operation.patient());
            op.putBoolean("Install", operation.install());
            op.putString("Slot", operation.slot().toString());
            op.putInt("Tier", operation.tier());
            op.putString("Type", operation.type().toString());
            op.putInt("Elapsed", elapsed);
            op.putInt("Total", operation.total());
            tag.put("Operation", op);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tank.readFromNBT(registries, tag.getCompound("Gel"));
        if (clientPacket) {
            clientOperation = readOperation(tag.getCompound("Operation"));
        }
    }

    private static RigView.Operation readOperation(CompoundTag op) {
        if (!op.hasUUID("Patient")) {
            return null;
        }
        SlotId slot = SlotId.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, net.minecraft.nbt.StringTag.valueOf(op.getString("Slot")))
                .result().orElse(null);
        ResourceLocation type = ResourceLocation.tryParse(op.getString("Type"));
        if (slot == null || type == null) {
            return null;
        }
        return new RigView.Operation(op.getUUID("Patient"), op.getBoolean("Install"), slot, op.getInt("Tier"), type,
                op.getInt("Elapsed"), Math.max(1, op.getInt("Total")));
    }

    /** Used by tests and the block's comparator: the implant type being operated on, if any. */
    public ResourceLocation operatingOn() {
        return operation == null ? null : operation.expected().isEmpty() ? null
                : ((ImplantItem) operation.expected().getItem()).typeId();
    }
}
