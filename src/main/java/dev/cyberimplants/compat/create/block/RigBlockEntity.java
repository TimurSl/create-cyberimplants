package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.cyberimplants.body.BodyOps;
import dev.cyberimplants.body.BodySync;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.implant.ImplantItem;
import dev.cyberimplants.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.UUID;

/**
 * Kinetic block entity of the Implantation Rig (docs/design/01-core-systems.md, section 1.5). Layer 2 operations are
 * timed (5/8/12/16/20 s by implant tier), need a minimum shaft speed (16/32/64/128/256 RPM) and 250 mB of gel (Bio-Interface
 * Gel for Tiers I-III, Neural Gel for IV-V), and abort when the Rig slows, the gel runs out or the patient moves.
 * Under {@code layers.maxEnabled = 1} they are instant and any rotation will do.
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
    private static final double MOVE_LIMIT_SQR = 1.5 * 1.5;

    private record Operation(UUID patient, boolean install, SlotId slot, int tier, int inventorySlot, ItemStack expected,
                             double x, double y, double z, int total) {
    }

    private final FluidTank tank = new FluidTank(TANK_CAPACITY,
            fluid -> fluid.is(ModFluids.BIO_GEL.get()) || fluid.is(ModFluids.NEURAL_GEL.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private Operation operation;
    private int elapsed;
    private int gelPaid;

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

    /** True when the player is close enough to this Rig and it is running. */
    public static boolean canOperate(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_USE_DISTANCE * MAX_USE_DISTANCE) {
            return false;
        }
        return player.level().getBlockEntity(pos) instanceof RigBlockEntity rig && rig.operational();
    }

    private static FluidStack gelFor(int tier) {
        return new FluidStack(tier <= 3 ? ModFluids.BIO_GEL.get() : ModFluids.NEURAL_GEL.get(), GEL_PER_OPERATION);
    }

    private static Component gelName(int tier) {
        return Component.translatable(tier <= 3 ? "fluid_type.cyberimplants.bio_interface_gel" : "fluid_type.cyberimplants.neural_gel");
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
        } else {
            InstalledImplant installed = body.installed().get(slot);
            BodyOps.Result r = BodyOps.canRemove(body, slot);
            if (installed == null || !r.ok()) {
                return Component.translatable(r.translationKey());
            }
            tier = installed.tier();
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
        int total = (int) Math.round(SECONDS[tier - 1] * 20 * ServerConfig.RIG_TIME_MULTIPLIER.get());
        operation = new Operation(player.getUUID(), install, slot, tier, inventorySlot, expected,
                player.getX(), player.getY(), player.getZ(), Math.max(1, total));
        elapsed = 0;
        gelPaid = 0;
        setChanged();
        return null;
    }

    // ---------------------------------------------------------------- running it

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || operation == null) {
            return;
        }
        Operation op = operation;
        Player found = level.getPlayerByUUID(op.patient());
        if (!(found instanceof ServerPlayer player) || !player.isAlive()) {
            finish(); // the patient is gone: nothing to hurt, nothing was changed
            return;
        }
        if (player.distanceToSqr(op.x(), op.y(), op.z()) > MOVE_LIMIT_SQR
                || player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)
                > MAX_USE_DISTANCE * MAX_USE_DISTANCE) {
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
        if (elapsed % 10 == 0) {
            player.displayClientMessage(Component.translatable("cyberimplants.rig.progress", 100 * elapsed / op.total()), true);
        }
        if (elapsed >= op.total()) {
            complete(player, op);
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
        } else {
            player.displayClientMessage(Component.translatable(result.translationKey()), true);
        }
        BodySync.markDirty(player);
        finish();
    }

    /** The patient takes the abort damage; the implant never left their inventory (or their body). */
    private void abort(ServerPlayer player, String reason, boolean hurt) {
        player.displayClientMessage(Component.translatable("cyberimplants.rig.abort." + reason), true);
        if (hurt) {
            player.hurt(player.damageSources().generic(), ABORT_DAMAGE);
        }
        finish();
    }

    private void finish() {
        operation = null;
        elapsed = 0;
        gelPaid = 0;
        setChanged();
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Gel", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tank.readFromNBT(registries, tag.getCompound("Gel"));
    }

    /** Used by tests and the block's comparator: the implant type being operated on, if any. */
    public ResourceLocation operatingOn() {
        return operation == null ? null : operation.expected().isEmpty() ? null
                : ((ImplantItem) operation.expected().getItem()).typeId();
    }
}
