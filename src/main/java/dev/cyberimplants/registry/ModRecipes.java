package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.compat.create.recipe.EtchingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Holds the {@code cyberimplants:etching} recipe type and serializer of the Precision Etcher (Layer 5). */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CyberImplants.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CyberImplants.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EtchingRecipe>> ETCHING_TYPE =
            RECIPE_TYPES.register("etching", () -> RecipeType.simple(CyberImplants.id("etching")));
    public static final DeferredHolder<RecipeSerializer<?>, EtchingRecipe.Serializer> ETCHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("etching", EtchingRecipe.Serializer::new);

    private ModRecipes() {
    }
}
