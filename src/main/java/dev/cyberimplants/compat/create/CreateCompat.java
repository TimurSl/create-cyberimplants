package dev.cyberimplants.compat.create;

import dev.cyberimplants.CyberImplants;
import net.neoforged.fml.ModList;

/**
 * The only seam between this mod and Create. Every other package talks to the bridge interfaces; only this package
 * (and its future implementations) may import {@code com.simibubi.create.*}.
 *
 * <p>Layer 0 ships no-op bridges (the interfaces' default methods). Layer 1 onward replaces them with real
 * implementations in {@link #init()}.
 */
public final class CreateCompat {
    private static final KineticBridge KINETIC = new KineticBridge() { };
    private static final BehaviourBridge BEHAVIOUR = new BehaviourBridge() { };
    private static final DisplaySourceBridge DISPLAY_SOURCE = new DisplaySourceBridge() { };
    private static final DisplayTargetBridge DISPLAY_TARGET = new DisplayTargetBridge() { };
    private static final LinkBridge LINK = new LinkBridge() { };
    private static final BacktankBridge BACKTANK = new BacktankBridge() { };
    private static final PotatoBridge POTATO = new PotatoBridge() { };
    private static final ContraptionBridge CONTRAPTION = new ContraptionBridge() { };
    private static final TrainBridge TRAIN = new TrainBridge() { };
    private static final LogisticsBridge LOGISTICS = new LogisticsBridge() { };
    private static final GoggleBridge GOGGLE = new GoggleBridge() { };

    private static boolean createLoaded;

    public static void init() {
        createLoaded = ModList.get().isLoaded("create");
        CyberImplants.LOGGER.info("Create compat initialised (Create present: {}; bridges: no-op stubs)", createLoaded);
    }

    public static boolean isCreateLoaded() {
        return createLoaded;
    }

    public static KineticBridge kinetic() { return KINETIC; }
    public static BehaviourBridge behaviour() { return BEHAVIOUR; }
    public static DisplaySourceBridge displaySource() { return DISPLAY_SOURCE; }
    public static DisplayTargetBridge displayTarget() { return DISPLAY_TARGET; }
    public static LinkBridge link() { return LINK; }
    public static BacktankBridge backtank() { return BACKTANK; }
    public static PotatoBridge potato() { return POTATO; }
    public static ContraptionBridge contraption() { return CONTRAPTION; }
    public static TrainBridge train() { return TRAIN; }
    public static LogisticsBridge logistics() { return LOGISTICS; }
    public static GoggleBridge goggles() { return GOGGLE; }

    private CreateCompat() {
    }
}
