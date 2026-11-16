package mod.crend.dynamiccrosshair.config;

public enum Policy {
    ALWAYS,
    IF_INTERACTABLE,
    DISABLED;

    public static Policy byName(String name) {
        if (name == null) return IF_INTERACTABLE;
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return IF_INTERACTABLE;
        }
    }
}
