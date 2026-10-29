package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.compat.create.block.ChargerBlock;
import dev.cyberimplants.compat.create.block.RigBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Implantation Rig and Kinetic Charger. Etcher, Neural Port, Relay Mast, Proximity Sensor and Terminal come later. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CyberImplants.MOD_ID);

    public static final DeferredBlock<RigBlock> IMPLANTATION_RIG = BLOCKS.register("implantation_rig",
            () -> new RigBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));

    public static final DeferredBlock<ChargerBlock> KINETIC_CHARGER = BLOCKS.register("kinetic_charger",
            () -> new ChargerBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));

    private ModBlocks() {
    }
}
