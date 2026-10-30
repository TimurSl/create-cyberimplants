package dev.cyberimplants.perf;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.config.ServerConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The server-wide deck budget (docs/design/08-technical-architecture.md, section 8.13) and its profiler. Every deck
 * thread asks for its cycle budget here: when the decks of the last tick would exceed {@code maxCyclesPerServerTick},
 * each gets an equal share, so throttling is fair instead of starving whoever ticks last. The last second of cycles,
 * VM time, throttled threads and dropped events is kept for {@code /cyberimplants perf}.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class DeckBudget {
    private static final int WINDOW = 20;

    private static long cycles;
    private static long nanos;
    private static int threads;
    private static int lastThreads = 1;
    private static int throttled;
    private static int dropped;

    private static final long[] CYCLES = new long[WINDOW];
    private static final long[] NANOS = new long[WINDOW];
    private static final int[] THREADS = new int[WINDOW];
    private static final int[] THROTTLED = new int[WINDOW];
    private static final int[] DROPPED = new int[WINDOW];
    private static int slot;

    /** A thread's cycles this tick: its own budget, capped by a fair share of what the server still allows. */
    public static int fairShare(int threadBudget, int serverCap, int threadsLastTick, long usedThisTick) {
        int share = serverCap / Math.max(1, threadsLastTick);
        long left = Math.max(0, serverCap - usedThisTick);
        return (int) Math.max(0, Math.min(threadBudget, Math.min(left, Math.max(share, 1))));
    }

    /** Called by a deck thread before it runs; returns its budget for this tick. */
    public static int claim(int threadBudget) {
        threads++;
        int granted = fairShare(threadBudget, ServerConfig.DECK_MAX_CYCLES_PER_SERVER_TICK.get(), lastThreads, cycles);
        if (granted < threadBudget) {
            throttled++;
        }
        return granted;
    }

    /** Called after a deck thread ran: the cycles it used and the wall time the VM took. */
    public static void record(int used, long elapsedNanos) {
        cycles += used;
        nanos += elapsedNanos;
    }

    public static void eventDropped() {
        dropped++;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        CYCLES[slot] = cycles;
        NANOS[slot] = nanos;
        THREADS[slot] = threads;
        THROTTLED[slot] = throttled;
        DROPPED[slot] = dropped;
        slot = (slot + 1) % WINDOW;
        lastThreads = Math.max(1, threads);
        cycles = 0;
        nanos = 0;
        threads = 0;
        throttled = 0;
        dropped = 0;
    }

    /** One line per budget: averages over the last second against the configured limits. */
    public static String report() {
        long c = 0;
        long n = 0;
        long peak = 0;
        int t = 0;
        int thr = 0;
        int drop = 0;
        for (int i = 0; i < WINDOW; i++) {
            c += CYCLES[i];
            n += NANOS[i];
            peak = Math.max(peak, CYCLES[i]);
            t = Math.max(t, THREADS[i]);
            thr += THROTTLED[i];
            drop += DROPPED[i];
        }
        return String.format("Deck VM, last second: %d cycles/tick on average (peak %d, server cap %d), %.3f ms/tick, "
                        + "%d threads, %d throttled thread-ticks, %d events dropped (cap %d per player per second)",
                c / WINDOW, peak, ServerConfig.DECK_MAX_CYCLES_PER_SERVER_TICK.get(), n / (double) WINDOW / 1.0e6, t, thr, drop,
                ServerConfig.DECK_MAX_EVENTS_PER_SECOND.get());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(CyberImplants.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("perf").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(report()), false);
                    return 1;
                })));
    }

    private DeckBudget() {
    }
}
