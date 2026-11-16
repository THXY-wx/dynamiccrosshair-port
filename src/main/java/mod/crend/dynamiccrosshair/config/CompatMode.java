package mod.crend.dynamiccrosshair.config;

/**
 * How to behave when Crosshair-Selection is installed and draws its own crosshair.
 */
public enum CompatMode {
    /** Detect Crosshair-Selection and yield while it is drawing. */
    AUTO,
    /** Always yield, even if detection fails. */
    YIELD,
    /** Draw our own crosshair and temporarily suspend Crosshair-Selection for the frame. */
    FORCE_OURS;

    public static CompatMode byName(String name) {
        if (name == null) return AUTO;
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
