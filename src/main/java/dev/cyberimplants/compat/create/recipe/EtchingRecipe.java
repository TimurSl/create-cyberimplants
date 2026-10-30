package dev.cyberimplants.compat.create.recipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.registry.ModBlocks;
import dev.cyberimplants.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@code cyberimplants:etching} (docs/design/05-create-integration.md, Precision Etcher): one item in, up to four out,
 * a processing time and the {@code min_rpm} the Etcher needs. Usable on its own and as a sequenced assembly step.
 */
public class EtchingRecipe extends ProcessingRecipe<SingleRecipeInput, EtchingParams> implements IAssemblyRecipe {
    /** Base processing time in ticks at 32 RPM when a recipe gives none; faster shafts etch proportionally faster. */
    public static final int DEFAULT_DURATION = 100;

    public static final IRecipeTypeInfo TYPE_INFO = new IRecipeTypeInfo() {
        @Override
        public ResourceLocation getId() {
            return CyberImplants.id("etching");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends RecipeSerializer<?>> T getSerializer() {
            return (T) ModRecipes.ETCHING_SERIALIZER.get();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
            return (RecipeType<R>) ModRecipes.ETCHING_TYPE.get();
        }
    };

    public EtchingRecipe(EtchingParams params) {
        super(TYPE_INFO, params);
    }

    public int minRpm() {
        return params.minRpm();
    }

    /** Ticks to etch at {@code rpm} (at least 1). */
    public int durationAt(float rpm) {
        int base = processingDuration > 0 ? processingDuration : DEFAULT_DURATION;
        return Math.max(1, (int) Math.ceil(base * 32.0 / Math.max(1.0f, Math.abs(rpm))));
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !ingredients.isEmpty() && ingredients.get(0).test(input.item());
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    public Component getDescriptionForAssembly() {
        return Component.translatable("cyberimplants.recipe.assembly.etching", minRpm());
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(ModBlocks.PRECISION_ETCHER.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    /** JEI draws an etching step like a pressing step (the Etcher works from the same spot above the belt). */
    @Override
    public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
        return () -> SequencedAssemblySubCategory.AssemblyPressing::new;
    }

    public static final class Serializer implements RecipeSerializer<EtchingRecipe> {
        private static final MapCodec<EtchingRecipe> CODEC = ProcessingRecipe.codec(EtchingRecipe::new, EtchingParams.CODEC);
        private static final StreamCodec<RegistryFriendlyByteBuf, EtchingRecipe> STREAM_CODEC =
                ProcessingRecipe.streamCodec(EtchingRecipe::new, EtchingParams.STREAM_CODEC);

        @Override
        public MapCodec<EtchingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EtchingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
