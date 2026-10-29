package dev.cyberimplants.fx;

/**
 * Screen-space effects the server asks one client to draw over its view (see {@code client/ClientFx}): a tint for the
 * Sandevistan field, static while camouflaged, a flash for the Defibrillator, screen shake ... Only the affected player
 * sees these; what everyone else sees is the particles and sounds in {@link ImplantFx}.
 */
public enum ScreenFx {
    SANDEVISTAN,
    KERENZIKOV,
    EVADE,
    CAMO,
    SCAN,
    TAG,
    DEFIB,
    ADRENALINE,
    SHELL,
    SHIELD_HIT,
    SHIELD_BREAK,
    SHOCK,
    GLITCH,
    ERS,
    DASH,
    LEAP,
    STRIKE,
    LANDING,
    HEAL;

    public static ScreenFx byName(String name) {
        for (ScreenFx fx : values()) {
            if (fx.name().equals(name)) {
                return fx;
            }
        }
        return null;
    }
}
