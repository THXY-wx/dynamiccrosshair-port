package mod.crend.dynamiccrosshair.config;

/**
 * Crosshair sprite shapes. The textures are the 15x15 sprites from DynamicCrosshair (LGPL-3.0,
 * by Crendgrim), redistributed here with attribution for the 1.7.10 port.
 */
public enum Shape {

    CROSSHAIR("crosshair"),
    CROSS_OPEN("cross-open"),
    CROSS_OPEN_DIAGONAL("cross-open-diagonal"),
    CROSS_DIAGONAL_SMALL("cross-diagonal-small"),
    DOT("dot"),
    CIRCLE("circle"),
    CIRCLE_LARGE("circle-large"),
    SQUARE("square"),
    SQUARE_LARGE("square-large"),
    DIAMOND("diamond"),
    DIAMOND_LARGE("diamond-large"),
    BRACKETS("brackets"),
    BRACKETS_ROUND("brackets-round"),
    BRACKETS_BOTTOM("brackets-bottom"),
    BRACKETS_TOP("brackets-top"),
    LINES("lines"),
    LINE_BOTTOM("line-bottom"),
    CARET("caret"),
    EMPTY("empty");

    public final String textureName;

    Shape(String textureName) {
        this.textureName = textureName;
    }

    public String getId() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Shape byId(String id) {
        if (id == null) return CROSSHAIR;
        String wanted = id.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        for (Shape shape : values()) {
            if (shape.name().equalsIgnoreCase(wanted)) return shape;
        }
        return CROSSHAIR;
    }
}
