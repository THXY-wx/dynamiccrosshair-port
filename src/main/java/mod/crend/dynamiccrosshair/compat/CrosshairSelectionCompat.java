package mod.crend.dynamiccrosshair.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import mod.crend.dynamiccrosshair.DynamicCrosshair;
import mod.crend.dynamiccrosshair.config.CompatMode;
import mod.crend.dynamiccrosshair.config.Config;
import cpw.mods.fml.common.Loader;

/**
 * Runtime bridge to Crosshair-Selection (me.kuionc.crosshairselection).
 *
 * <p>The point of this bridge is <b>not</b> to hide one of the two crosshairs: it lets this mod draw
 * its own dynamic shapes (and bobbing) while reusing Crosshair-Selection's colour, outline and
 * flowing-rainbow parameters. Crosshair-Selection's own renderer is suspended for the frame so the
 * two never draw at the same time.</p>
 *
 * <p>Everything is reflective: the mod works without Crosshair-Selection installed and also when
 * its internal fields change (the tint simply falls back to this mod's own colours).</p>
 */
public final class CrosshairSelectionCompat {

    private static final String CONFIG_CLASS = "me.kuionc.crosshairselection.config.CrosshairConfig";
    private static final String FLOW_CLASS = "me.kuionc.crosshairselection.config.FlowSettings";

    private static boolean initialised;
    private static boolean present;
    private static boolean warned;

    private static Field enabledField;
    private static Field colourField;
    private static Field outlineField;
    private static Field outlineColourField;
    private static Field outlineThicknessField;
    private static Field thicknessField;
    private static Field dotField;
    private static Field dotColourField;
    private static Field flowField;
    private static Field bowIndicatorField;
    private static Method ticProgressMethod;
    private static Method drawArrowMethod;
    private static Method drawNumberMethod;

    private static Field flowModeField;
    private static Field flowMotionField;
    private static Field flowDirectionField;
    private static Field hueStartField;
    private static Field hueRangeField;
    private static Field saturationField;
    private static Field brightnessField;
    private static Field opacityField;
    private static Field speedField;
    private static Field densityField;
    private static Field softnessField;
    private static Field progressField;

    private static Boolean suspendedOriginal;
    private static Boolean suspendedDot;
    private static final Tint TINT = new Tint();

    private CrosshairSelectionCompat() {}

    private static void init() {
        initialised = true;
        if (!Loader.isModLoaded("crosshairselection")) return;
        try {
            Class<?> config = Class.forName(CONFIG_CLASS);
            enabledField = field(config, "enabled");
            colourField = field(config, "colour");
            outlineField = field(config, "outline");
            outlineColourField = field(config, "outlineColour");
            outlineThicknessField = field(config, "outlineThickness");
            thicknessField = field(config, "thickness");
            dotField = field(config, "dot");
            dotColourField = field(config, "dotColour");
            flowField = field(config, "flow");

            Class<?> flow = Class.forName(FLOW_CLASS);
            flowModeField = field(flow, "mode");
            flowMotionField = field(flow, "motion");
            flowDirectionField = field(flow, "direction");
            hueStartField = field(flow, "hueStart");
            hueRangeField = field(flow, "hueRange");
            saturationField = field(flow, "saturation");
            brightnessField = field(flow, "brightness");
            opacityField = field(flow, "opacity");
            speedField = field(flow, "speed");
            densityField = field(flow, "bandDensity");
            softnessField = field(flow, "sweepSoftness");
            progressField = field(flow, "progress");

            try {
                Class<?> hud = Class.forName("me.kuionc.crosshairselection.render.CrosshairHud");
                drawArrowMethod = hud.getDeclaredMethod("drawArrow",
                        net.minecraft.client.Minecraft.class, float.class, float.class, float.class);
                drawArrowMethod.setAccessible(true);
                drawNumberMethod = hud.getDeclaredMethod("drawNumber",
                        net.minecraft.client.Minecraft.class, float.class, float.class, float.class);
                drawNumberMethod.setAccessible(true);
                bowIndicatorField = field(config, "bowChargeIndicator");
                Class<?> tic = Class.forName("me.kuionc.crosshairselection.compat.TicCrosshairCompat");
                ticProgressMethod = tic.getMethod("progress", net.minecraft.entity.player.EntityPlayer.class);
            } catch (Throwable ignored) {
                drawArrowMethod = null;
                drawNumberMethod = null;
                bowIndicatorField = null;
                ticProgressMethod = null;
            }

            present = true;
            Runtime.getRuntime().addShutdownHook(new Thread(CrosshairSelectionCompat::emergencyRestore,
                    "dynamiccrosshair-cs-restore"));
            DynamicCrosshair.LOGGER.info("Crosshair-Selection detected; colour/flow bridge active (mode {})", Config.crosshairSelection);
        } catch (Throwable t) {
            warn("Failed to hook Crosshair-Selection: " + t);
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isPresent() {
        if (!initialised) init();
        return present;
    }

    /** AUTO mode: draw our shapes with Crosshair-Selection's colour/flow. */
    public static boolean shouldDrive() {
        if (!Config.enabled) return false;
        if (Config.crosshairSelection != CompatMode.AUTO) return false;
        if (!isPresent()) return false;
        // The bridge suspends Crosshair-Selection for the render frame; keep the pre-suspend value
        // so HUD-time decisions still see the user's setting.
        if (suspendedOriginal != null) return suspendedOriginal;
        return booleanValue(enabledField, false);
    }

    /**
     * Draws Crosshair-Selection's own bow/crossbow charge indicator (arrow or percentage) at the
     * given crosshair centre. Returns true when Crosshair-Selection handled it, so the caller does
     * not draw its local fallback mark.
     */
    public static boolean drawBowIndicator(net.minecraft.client.Minecraft mc, float centreX, float centreY) {
        if (!isPresent() || mc == null || mc.thePlayer == null) return false;
        if (bowIndicatorField == null || ticProgressMethod == null
                || drawArrowMethod == null || drawNumberMethod == null) {
            return false;
        }
        try {
            Object indicator = bowIndicatorField.get(null);
            String mode = indicator instanceof Enum ? ((Enum<?>) indicator).name() : "NONE";
            if ("NONE".equals(mode)) return true;
            Object value = ticProgressMethod.invoke(null, mc.thePlayer);
            if (!(value instanceof Number)) return false;
            float charge = ((Number) value).floatValue();
            if (charge <= 0.001F) return true;
            if ("ARROW".equals(mode)) {
                drawArrowMethod.invoke(null, mc, centreX, centreY, charge);
            } else {
                drawNumberMethod.invoke(null, mc, centreX, centreY, charge);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** YIELD mode: leave the crosshair completely to Crosshair-Selection. */
    public static boolean shouldYield() {
        if (!Config.enabled) return false;
        if (!isPresent()) return false;
        return Config.crosshairSelection == CompatMode.YIELD;
    }

    /**
     * Crosshair-Selection itself is switched off for the whole frame while this mod draws, so its
     * RenderTick handler does not paint a second crosshair.
     */
    public static void suspendForFrame() {
        if (!isPresent() || suspendedOriginal != null) return;
        // Never touch Crosshair-Selection while a GUI is open: its config screen would read the
        // temporary false value and persist it when the screen is closed.
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen != null) return;
        boolean suspend = Config.crosshairSelection == CompatMode.FORCE_OURS || shouldDrive();
        if (!suspend) return;
        try {
            suspendedOriginal = enabledField.getBoolean(null);
            enabledField.setBoolean(null, false);
            if (dotField != null) {
                suspendedDot = dotField.getBoolean(null);
                dotField.setBoolean(null, false);
            }
        } catch (Throwable t) {
            warn("Failed to suspend Crosshair-Selection: " + t);
        }
    }

    public static void emergencyRestore() {
        if (suspendedOriginal == null) return;
        try {
            if (enabledField != null) enabledField.setBoolean(null, suspendedOriginal);
            if (suspendedDot != null && dotField != null) dotField.setBoolean(null, suspendedDot);
        } catch (Throwable ignored) {
            // best effort during shutdown
        } finally {
            suspendedOriginal = null;
            suspendedDot = null;
        }
    }

    public static void restoreAfterFrame() {
        if (suspendedOriginal == null) return;
        try {
            enabledField.setBoolean(null, suspendedOriginal);
            if (suspendedDot != null && dotField != null) {
                dotField.setBoolean(null, suspendedDot);
            }
        } catch (Throwable t) {
            warn("Failed to restore Crosshair-Selection: " + t);
        } finally {
            suspendedOriginal = null;
            suspendedDot = null;
        }
    }

    /** Snapshot of everything needed to colourise one frame. */
    public static Tint readTint(int guiScale, int screenHeight, float centreFragY) {
        Tint tint = TINT;
        tint.guiScale = Math.max(1, guiScale);
        tint.screenHeight = Math.max(1, screenHeight);
        tint.centreFragY = centreFragY;
        tint.colour = intValue(colourField, 0xFFFFFFFF);
        tint.outline = booleanValue(outlineField, false);
        tint.outlineColour = intValue(outlineColourField, 0xFF000000);
        tint.outlineThickness = floatValue(outlineThicknessField, 0.5F);
        tint.thickness = intValue(thicknessField, 3);
        tint.dot = booleanValue(dotField, false);
        tint.dotColour = intValue(dotColourField, 0xFF000000);

        Object flow = objectValue(flowField);
        if (flow == null) {
            tint.flow = false;
            return tint;
        }
        String mode = enumName(flowModeField, flow, "OFF");
        tint.flow = !"OFF".equals(mode);
        tint.flowMode = "SWEEP".equals(mode) ? 2 : 1;
        String direction = enumName(flowDirectionField, flow, "FORWARD");
        tint.reverse = "REVERSE".equals(direction);
        tint.motion = motionOrdinal(enumName(flowMotionField, flow, "COSINE"));
        tint.hueStart = floatValue(hueStartField, flow, 0.26F);
        tint.hueRange = floatValue(hueRangeField, flow, 0.32F);
        tint.saturation = floatValue(saturationField, flow, 0.49F);
        tint.brightness = floatValue(brightnessField, flow, 0.91F);
        tint.opacity = floatValue(opacityField, flow, 1.0F);
        tint.speed = floatValue(speedField, flow, 500.0F);
        tint.density = floatValue(densityField, flow, 0.35F);
        tint.softness = floatValue(softnessField, flow, 0.35F);
        tint.progress = floatValue(progressField, flow, 1.0F);
        return tint;
    }

    /** Colours produced for one frame: Crosshair-Selection's colour plus its flowing rainbow. */
    public static final class Tint {

        public int colour = 0xFFFFFFFF;
        public boolean flow;
        public int flowMode;
        public int motion;
        public boolean reverse;
        public float hueStart = 0.26F;
        public float hueRange = 0.32F;
        public float saturation = 0.49F;
        public float brightness = 0.91F;
        public float opacity = 1.0F;
        public float speed = 500.0F;
        public float density = 0.35F;
        public float softness = 0.35F;
        public float progress = 1.0F;
        public boolean outline;
        public int outlineColour = 0xFF000000;
        public float outlineThickness = 0.5F;
        public boolean dot;
        public int dotColour = 0xFF000000;
        public int thickness = 3;
        public int guiScale = 1;
        public int screenHeight = 480;
        public float centreFragY;

        /** ARGB for a point given in physical pixels, X right and Y up from the centre. */
        public int colourAt(float dxPx, float dyPx, long now) {
            int baseAlpha = (colour >>> 24) & 0xFF;
            if (!flow) {
                return (baseAlpha << 24) | (colour & 0x00FFFFFF);
            }
            float phase = motionPhase(phase(now), motion);
            float band = (dxPx + dyPx) * 0.01F * Math.max(density, 0.01F);
            float hue = hueStart + fract(band + phase) * hueRange;
            int rgb = hsv(fract(hue), clamp01(saturation), clamp01(brightness));

            if (flowMode == 2) {
                float fragY = centreFragY + dyPx;
                float height01 = fragY / screenHeight;
                float sweep = height01 - progress;
                float mask = sweep <= 0.0F ? 1.0F : 1.0F - smoothstep(0.0F, Math.max(softness, 0.001F), sweep);
                rgb = mix((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, (rgb >> 16) & 0xFF,
                        (rgb >> 8) & 0xFF, rgb & 0xFF, clamp01(mask));
            }
            int alpha = (int) (baseAlpha * clamp01(opacity));
            return (alpha << 24) | (rgb & 0x00FFFFFF);
        }

        private float phase(long now) {
            double value = now * 0.001D * (speed / 1000.0D);
            if (reverse) value = -value;
            value -= Math.floor(value);
            return (float) value;
        }

        private static float motionPhase(float x, int mode) {
            if (mode == 1) {
                float w = fract(x);
                return w - (float) Math.sin(6.28318530718F * w) / 6.28318530718F;
            }
            if (mode == 2) {
                return Math.abs(fract(x) * 2.0F - 1.0F);
            }
            return fract(x);
        }
    }

    // ------------------------------------------------------------------ reflection helpers

    private static Object objectValue(Field field) {
        if (field == null) return null;
        try {
            return field.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean booleanValue(Field field, boolean fallback) {
        if (field == null) return fallback;
        try {
            return field.getBoolean(null);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static int intValue(Field field, int fallback) {
        if (field == null) return fallback;
        try {
            return field.getInt(null);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static float floatValue(Field field, float fallback) {
        if (field == null) return fallback;
        try {
            return field.getFloat(null);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static float floatValue(Field field, Object owner, float fallback) {
        if (field == null || owner == null) return fallback;
        try {
            return field.getFloat(owner);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static String enumName(Field field, Object owner, String fallback) {
        if (field == null || owner == null) return fallback;
        try {
            Object value = field.get(owner);
            return value instanceof Enum ? ((Enum<?>) value).name() : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static int motionOrdinal(String name) {
        if ("LINEAR".equals(name)) return 0;
        if ("COSINE".equals(name)) return 1;
        if ("TRIANGLE".equals(name)) return 2;
        if ("NOISE".equals(name)) return 3;
        return 1;
    }

    // ------------------------------------------------------------------ colour maths (same as the CS shader)

    private static float fract(float value) {
        return value - (float) Math.floor(value);
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0F - 2.0F * t);
    }

    private static int mix(int ar, int ag, int ab, int br, int bg, int bb, float t) {
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int b = (int) (ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** HSV (all 0..1) to 0xRRGGBB. */
    private static int hsv(float h, float s, float v) {
        float c = v * s;
        float hp = h * 6.0F;
        float x = c * (1.0F - Math.abs(hp % 2.0F - 1.0F));
        float r;
        float g;
        float b;
        int sector = (int) Math.floor(hp) % 6;
        switch (sector) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        float m = v - c;
        return ((int) ((r + m) * 255.0F) << 16) | ((int) ((g + m) * 255.0F) << 8) | (int) ((b + m) * 255.0F);
    }

    private static void warn(String message) {
        if (warned) return;
        warned = true;
        DynamicCrosshair.LOGGER.warn(message);
    }
}
