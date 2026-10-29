package dev.cyberimplants.synapse.compiler;

/** Operand of {@link Op#DEVICE}: what kind of device and the label it was bound under. */
public record DeviceInfo(String kind, String label) {
}
