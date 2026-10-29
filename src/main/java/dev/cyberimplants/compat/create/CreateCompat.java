package dev.cyberimplants.compat.create;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.implant.ImplantIds;
import net.neoforged.fml.ModList;

/**
 * The only seam between this mod and Create. Every other package talks to the bridge interfaces; only this package
 * (and its future implementations) may import {@code com.simibubi.create.*}.
 *
 * <p>Bridges default to no-ops (the interfaces' default methods); real implementations replace them in
 * {@link #init()} as layers need them. Layer 1 adds the kinetic (speed) and goggle bridges. The Rig and Charger blocks live in
 * {@code compat.create.block} because they extend Create's kinetic classes.
 */
public final class CreateCompat {
    private static volatile KineticBridge kinetic = new KineticBridge() { };
    private static final BehaviourBridge BEHAVIOUR = new BehaviourBridge() { };
    private static final DisplaySourceBridge DISPLAY_SOURCE = new DisplaySourceBridge() { };
    private static final DisplayTargetBridge DISPLAY_TARGET = new DisplayTargetBridge() { };
    private static final LinkBridge LINK = new LinkBridge() { };
    private static final BacktankBridge BACKTANK = new BacktankBridge() { };
    private static final PotatoBridge POTATO = new PotatoBridge() { };
    private static final ContraptionBridge CONTRAPTION = new ContraptionBridge() { };
    private static final TrainBridge TRAIN = new TrainBridge() { };
    private static final LogisticsBridge LOGISTICS = new LogisticsBridge() { };
    private static volatile GoggleBridge goggle = new GoggleBridge() { };

    private static boolean createLoaded;

    public static void init() {
        createLoaded = ModList.get().isLoaded("create");
        if (createLoaded) {
            // Layer 1: the Machine Analyzer counts as Engineer's Goggles.
            kinetic = new KineticBridgeImpl();
            goggle = new GoggleBridgeImpl();
            goggle.registerWearingPredicate(player -> BodyState.get(player).has(ImplantIds.MACHINE_ANALYZER));
        }
        CyberImplants.LOGGER.info("Create compat initialised (Create present: {}; real bridges: kinetic, goggles)", createLoaded);
    }

    public static boolean isCreateLoaded() {
        return createLoaded;
    }

    public static KineticBridge kinetic() { return kinetic; }
    public static BehaviourBridge behaviour() { return BEHAVIOUR; }
    public static DisplaySourceBridge displaySource() { return DISPLAY_SOURCE; }
    public static DisplayTargetBridge displayTarget() { return DISPLAY_TARGET; }
    public static LinkBridge link() { return LINK; }
    public static BacktankBridge backtank() { return BACKTANK; }
    public static PotatoBridge potato() { return POTATO; }
    public static ContraptionBridge contraption() { return CONTRAPTION; }
    public static TrainBridge train() { return TRAIN; }
    public static LogisticsBridge logistics() { return LOGISTICS; }
    public static GoggleBridge goggles() { return goggle; }

    private CreateCompat() {
    }
}
