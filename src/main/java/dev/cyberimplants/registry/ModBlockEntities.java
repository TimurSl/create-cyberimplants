package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.compat.create.block.ChargerBlockEntity;
import dev.cyberimplants.compat.create.block.RigBlockEntity;
import dev.cyberimplants.content.NeuralPortBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CyberImplants.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RigBlockEntity>> RIG =
            BLOCK_ENTITY_TYPES.register("implantation_rig", () ->
                    BlockEntityType.Builder.of((pos, state) -> new RigBlockEntity(ModBlockEntities.RIG.get(), pos, state),
                            ModBlocks.IMPLANTATION_RIG.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER =
            BLOCK_ENTITY_TYPES.register("kinetic_charger", () ->
                    BlockEntityType.Builder.of((pos, state) -> new ChargerBlockEntity(ModBlockEntities.CHARGER.get(), pos, state),
                            ModBlocks.KINETIC_CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NeuralPortBlockEntity>> NEURAL_PORT =
            BLOCK_ENTITY_TYPES.register("neural_port", () ->
                    BlockEntityType.Builder.of(NeuralPortBlockEntity::new, ModBlocks.NEURAL_PORT.get()).build(null));

    private ModBlockEntities() {
    }
}
