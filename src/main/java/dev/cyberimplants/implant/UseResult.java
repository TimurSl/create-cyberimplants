package dev.cyberimplants.implant;

/** Outcome of using an implant. Failures double as the deck's {@code last_error} codes. */
public enum UseResult {
    OK,
    NO_CHARGE,
    COOLDOWN,
    BUSY,
    RANGE,
    NO_LOS,
    DENIED,
    MISSING,
    /** Knocked out by a Strain glitch for a few seconds. */
    OFFLINE;

    public boolean ok() {
        return this == OK;
    }

    public String translationKey() {
        return "cyberimplants.use." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
