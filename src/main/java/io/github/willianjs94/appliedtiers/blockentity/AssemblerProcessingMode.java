package io.github.willianjs94.appliedtiers.blockentity;

public enum AssemblerProcessingMode {
    AUTOMATIC("automatic"),
    EXCLUSIVE("exclusive"),
    DIVERSIFIED("diversified");

    private final String id;

    AssemblerProcessingMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public AssemblerProcessingMode next() {
        return switch (this) {
            case AUTOMATIC -> EXCLUSIVE;
            case EXCLUSIVE -> DIVERSIFIED;
            case DIVERSIFIED -> AUTOMATIC;
        };
    }

    public static AssemblerProcessingMode fromId(String id) {
        for (var mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return AUTOMATIC;
    }
}
