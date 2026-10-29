package dev.cyberimplants.implant;

/** Rule flags an implant type can carry. */
public enum ImplantFlag {
    /** May draw on the Energy Buffer; eligible for Emergency Response System activation. */
    EMERGENCY,
    /** May draw pressurised air from a Create backtank instead of Charge. */
    PNEUMATIC,
    /** A Cybernetic Arm frame: installs into either arm region. */
    ARM_FRAME,
    /** A limb module: installs into a socket of an installed arm frame. */
    ARM_MODULE,
    /** A cyberdeck module: installs into the chassis' sockets and costs +1 Load. */
    DECK_MODULE
}
