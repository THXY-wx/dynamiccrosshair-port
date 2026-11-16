package mod.crend.dynamiccrosshair.config;

/**
 * A single crosshair style: sprite, ARGB tint, blending, coalescing and scale.
 *
 * <p>Serialised as {@code shape:argb:blend:coalesce:scale}, for example
 * {@code cross_open:0xFFFFFFFF:true:true:1.0}.</p>
 */
public class StyleSetting {

    public Shape shape;
    public int color;
    public boolean blend;
    public boolean coalesce;
    public float scale;

    public StyleSetting(Shape shape, int color, boolean blend, boolean coalesce, float scale) {
        this.shape = shape;
        this.color = color;
        this.blend = blend;
        this.coalesce = coalesce;
        this.scale = scale;
    }

    public StyleSetting copy() {
        return new StyleSetting(shape, color, blend, coalesce, scale);
    }

    public StyleSetting copyFrom(StyleSetting other) {
        this.shape = other.shape;
        this.color = other.color;
        this.blend = other.blend;
        this.coalesce = other.coalesce;
        this.scale = other.scale;
        return this;
    }

    public String serialize() {
        return shape.getId() + ":0x" + String.format("%08X", color) + ":" + blend + ":" + coalesce + ":" + scale;
    }

    public static StyleSetting parse(String value, StyleSetting fallback) {
        if (value == null) return fallback.copy();
        try {
            String[] parts = value.split(":");
            if (parts.length < 2) return fallback.copy();
            Shape shape = Shape.byId(parts[0]);
            String colour = parts[1].trim();
            int argb = (int) Long.parseLong(colour.startsWith("0x") || colour.startsWith("0X") ? colour.substring(2) : colour, 16);
            boolean blend = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : true;
            boolean coalesce = parts.length > 3 ? Boolean.parseBoolean(parts[3]) : true;
            float scale = parts.length > 4 ? Float.parseFloat(parts[4]) : 1.0F;
            if (scale <= 0.05F) scale = 0.05F;
            if (scale > 8.0F) scale = 8.0F;
            return new StyleSetting(shape, argb, blend, coalesce, scale);
        } catch (RuntimeException e) {
            return fallback.copy();
        }
    }

    public float red() {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (color & 0xFF) / 255.0F;
    }

    public float alpha() {
        return ((color >> 24) & 0xFF) / 255.0F;
    }
}
