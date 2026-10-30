package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * The process fluids (docs/design/05-create-integration.md, section 5.3): Bio-Interface Gel and Neural Gel for surgery,
 * Conductive Ink, Cryo-Coolant and Nanite Suspension for the component chain. Each one registers its fluid type, source
 * and flowing fluid, liquid block and bucket under the same name; textures are textures/block/&lt;name&gt;_still and _flow.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CyberImplants.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CyberImplants.MOD_ID);

    /** One process fluid and everything registered for it. {@link #get()} is the source fluid. */
    public static final class ProcessFluid implements Supplier<BaseFlowingFluid.Source> {
        private final String name;
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        private final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        private final DeferredBlock<LiquidBlock> block;
        private final DeferredItem<BucketItem> bucket;

        private ProcessFluid(String name, int density, int viscosity, int light) {
            this.name = name;
            this.type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type." + CyberImplants.MOD_ID + "." + name)
                    .density(density).viscosity(viscosity).lightLevel(light)));
            this.source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
            this.flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
            this.block = ModBlocks.BLOCKS.register(name, () -> new LiquidBlock(source().get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(s -> light)));
            this.bucket = ModItems.ITEMS.register(name + "_bucket", () -> new BucketItem(source().get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        }

        // Read through methods: the suppliers above run long after construction, when every holder is assigned.
        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type(), source(), flowing())
                    .bucket(bucket()).block(block()).slopeFindDistance(2).levelDecreasePerBlock(2);
        }

        public String name() {
            return name;
        }

        public DeferredHolder<FluidType, FluidType> type() {
            return type;
        }

        public DeferredHolder<Fluid, BaseFlowingFluid.Source> source() {
            return source;
        }

        public DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing() {
            return flowing;
        }

        public DeferredBlock<LiquidBlock> block() {
            return block;
        }

        public DeferredItem<BucketItem> bucket() {
            return bucket;
        }

        @Override
        public BaseFlowingFluid.Source get() {
            return source.get();
        }
    }

    public static final ProcessFluid BIO_GEL = new ProcessFluid("bio_interface_gel", 1200, 3000, 0);
    public static final ProcessFluid NEURAL_GEL = new ProcessFluid("neural_gel", 1400, 4000, 6);
    public static final ProcessFluid CONDUCTIVE_INK = new ProcessFluid("conductive_ink", 1100, 1500, 2);
    public static final ProcessFluid CRYO_COOLANT = new ProcessFluid("cryo_coolant", 1050, 1200, 0);
    public static final ProcessFluid NANITE_SUSPENSION = new ProcessFluid("nanite_suspension", 1300, 2500, 4);

    public static final List<ProcessFluid> ALL = List.of(BIO_GEL, NEURAL_GEL, CONDUCTIVE_INK, CRYO_COOLANT, NANITE_SUSPENSION);

    private ModFluids() {
    }
}
