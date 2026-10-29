package dev.cyberimplants.implant;

/** Rule flags an implant type can carry. */
public enum ImplantFlag {
    /** May draw on the Energy Buffer; eligible for Emergency Response System activation. */
    EMERGENCY,
    /** May draw pressurised air from a Create backtank instead of Charge. */
    PNEUMATIC
}
