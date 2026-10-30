package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.cyberimplants.compat.create.recipe.EtchingRecipe;
import dev.cyberimplants.registry.ModRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Etches the item held on the belt or depot two blocks below (the Mechanical Press's spot). A recipe only runs at or
 * above its {@code min_rpm}; below that the item passes and the goggles say why. Etching time scales with speed.
 */
public class EtcherBlockEntity extends KineticBlockEntity {
    /** Stress impact per RPM (docs/design/05-create-integration.md: 8 SU/RPM). */
    public static final float STRESS_IMPACT = 8.0f;

    private int progress;
    private int tooSlowFor;

    public EtcherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new BeltProcessingBehaviour(this).whenItemEnters(this::onItemEntered).whileItemHeld(this::whileHeld));
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }

    /** The etching recipe for a stack: the current sequenced assembly step first, then plain etching recipes. */
    public Optional<RecipeHolder<EtchingRecipe>> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        Optional<RecipeHolder<EtchingRecipe>> step =
                SequencedAssemblyRecipe.getRecipe(level, stack, ModRecipes.ETCHING_TYPE.get(), EtchingRecipe.class);
        if (step.isPresent()) {
            return step;
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.ETCHING_TYPE.get(), new SingleRecipeInput(stack), level);
    }

    private boolean fastEnough(EtchingRecipe recipe) {
        boolean ok = Math.abs(getSpeed()) >= Math.max(1, recipe.minRpm()) && !isOverStressed();
        tooSlowFor = ok ? 0 : recipe.minRpm();
        return ok;
    }

    private ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        Optional<RecipeHolder<EtchingRecipe>> recipe = recipeFor(transported.stack);
        if (recipe.isEmpty() || !fastEnough(recipe.get().value())) {
            return ProcessingResult.PASS;
        }
        progress = 0;
        sendData();
        return ProcessingResult.HOLD;
    }

    private ProcessingResult whileHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        Optional<RecipeHolder<EtchingRecipe>> holder = recipeFor(transported.stack);
        if (holder.isEmpty() || !fastEnough(holder.get().value())) {
            progress = 0;
            return ProcessingResult.PASS;
        }
        EtchingRecipe recipe = holder.get().value();
        progress++;
        if (level instanceof ServerLevel server && progress % 4 == 0) {
            Vec3 at = handler.getWorldPositionOf(transported);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.15, at.z, 3, 0.12, 0.02, 0.12, 0.02);
        }
        if (progress < recipe.durationAt(getSpeed())) {
            return ProcessingResult.HOLD;
        }
        progress = 0;
        List<TransportedItemStack> outputs = new ArrayList<>();
        for (ItemStack result : recipe.rollResults(level.random)) {
            TransportedItemStack out = transported.copy();
            out.stack = result;
            outputs.add(out);
        }
        level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.4f, 1.8f);
        sendData();
        // One item per pass: the rest of a stack stays held for the next etch, the products move on.
        if (transported.stack.getCount() > 1) {
            TransportedItemStack left = transported.copy();
            left.stack.shrink(1);
            handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(outputs, left));
            return ProcessingResult.HOLD;
        }
        handler.handleProcessingOnItem(transported, TransportedResult.convertTo(outputs));
        return ProcessingResult.PASS;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (tooSlowFor > 0) {
            tooltip.add(Component.literal("    ").append(Component.translatable("cyberimplants.etcher.too_slow", tooSlowFor)
                    .withStyle(ChatFormatting.GOLD)));
            return true;
        }
        return added;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Progress", progress);
        tag.putInt("TooSlowFor", tooSlowFor);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        progress = tag.getInt("Progress");
        tooSlowFor = tag.getInt("TooSlowFor");
    }
}
