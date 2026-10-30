package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.compat.create.block.ChargerBlock;
import dev.cyberimplants.compat.create.block.EtcherBlock;
import dev.cyberimplants.compat.create.block.RigBlock;
import dev.cyberimplants.content.NeuralPortBlock;
import dev.cyberimplants.content.ProximitySensorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Implantation Rig, Kinetic Charger, Precision Etcher, Neural Port and Proximity Sensor. Fluid blocks are registered by {@link ModFluids}. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CyberImplants.MOD_ID);

    public static final DeferredBlock<RigBlock> IMPLANTATION_RIG = BLOCKS.register("implantation_rig",
            () -> new RigBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));

    public static final DeferredBlock<ChargerBlock> KINETIC_CHARGER = BLOCKS.register("kinetic_charger",
            () -> new ChargerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));

    public static final DeferredBlock<EtcherBlock> PRECISION_ETCHER = BLOCKS.register("precision_etcher",
            () -> new EtcherBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));

    public static final DeferredBlock<NeuralPortBlock> NEURAL_PORT = BLOCKS.register("neural_port",
            () -> new NeuralPortBlock(BlockBehaviour.Properties.of()
                    .strength(1.5f, 4.0f).sound(SoundType.METAL).noOcclusion().noCollission()));

    public static final DeferredBlock<ProximitySensorBlock> PROXIMITY_SENSOR = BLOCKS.register("proximity_sensor",
            () -> new ProximitySensorBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    private ModBlocks() {
    }
}
