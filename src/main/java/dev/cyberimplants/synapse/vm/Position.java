package dev.cyberimplants.synapse.vm;

/** A position value ({@code POS(x, y, z)}). */
public record Position(double x, double y, double z) implements Handle {
    @Override
    public Object get(String member) {
        return switch (member) {
            case "x" -> x;
            case "y" -> y;
            case "z" -> z;
            default -> null;
        };
    }

    @Override
    public int readCost() {
        return 1;
    }

    public double distanceTo(Position other) {
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
