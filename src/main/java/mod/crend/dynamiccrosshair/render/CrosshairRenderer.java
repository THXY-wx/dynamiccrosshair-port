package mod.crend.dynamiccrosshair.render;

import java.util.EnumMap;
import java.util.Map;

import mod.crend.dynamiccrosshair.compat.CrosshairSelectionCompat.Tint;
import mod.crend.dynamiccrosshair.config.Shape;
import mod.crend.dynamiccrosshair.config.StyleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Draws the 15x15 crosshair sprites, either with their own style or with Crosshair-Selection's tint. */
public final class CrosshairRenderer {

    private static final Map<Shape, ResourceLocation> TEXTURES = new EnumMap<>(Shape.class);
    private static final int FLOW_CELLS = 6;

    private CrosshairRenderer() {}

    public static void init() {
        for (Shape shape : Shape.values()) {
            TEXTURES.put(shape, new ResourceLocation("dynamiccrosshair",
                    "textures/gui/crosshair/" + shape.textureName + ".png"));
        }
    }

    /** Standalone style: the sprite's configured colour and blend mode. */
    public static void draw(StyleSetting style, float centerX, float centerY) {
        draw(style, centerX, centerY, 0.0F);
    }

    /** Standalone style with an extra central gap (bow/crossbow wind-up). */
    public static void draw(StyleSetting style, float centerX, float centerY, float gap) {
        if (style == null || style.color == 0) return;
        begin(style.blend);
        if (gap >= 0.5F) {
            drawSplit(style, centerX, centerY, gap, style.color, null, 0L);
        } else {
            drawSolid(style, centerX, centerY, 1.0F, style.color, 1.0F);
        }
        end();
    }

    /** Modifier overlays (correct/incorrect tool) keep their own solid colour. */
    public static void drawModifier(StyleSetting style, float centerX, float centerY) {
        if (style == null || style.color == 0) return;
        begin(false);
        drawSolid(style, centerX, centerY, 1.0F, style.color, 1.0F);
        end();
    }

    /**
     * Dynamic style driven through Crosshair-Selection's palette: the shape comes from this mod,
     * while the colour, the outline and the flowing rainbow come from Crosshair-Selection.
     */
    public static void drawTinted(StyleSetting style, float centerX, float centerY, Tint tint) {
        drawTinted(style, centerX, centerY, tint, 0.0F);
    }

    public static void drawTinted(StyleSetting style, float centerX, float centerY, Tint tint, float gap) {
        if (style == null || tint == null) return;
        if (((tint.colour >>> 24) & 0xFF) == 0 && !tint.flow) return;

        begin(false);
        if (gap < 0.5F && mod.crend.dynamiccrosshair.config.Config.adoptSelectionOutline
                && tint.outline && tint.outlineThickness > 0.01F && (tint.outlineColour >>> 24) != 0) {
            float expansion = 1.0F + (tint.outlineThickness * 2.0F) / 15.0F;
            drawSolid(style, centerX, centerY, expansion, tint.outlineColour, 1.0F);
        }
        long now = System.currentTimeMillis();
        if (gap >= 0.5F) {
            drawSplit(style, centerX, centerY, gap, tint.colour, tint, now);
        } else if (tint.flow) {
            drawFlow(style, centerX, centerY, tint, now);
        } else {
            drawSolid(style, centerX, centerY, 1.0F, tint.colour, 1.0F);
        }
        if (tint.dot && (tint.dotColour >>> 24) != 0) {
            drawDot(centerX, centerY, tint.dotColour, Math.max(1.0F, tint.thickness * 0.25F));
        }
        end();
    }

    private static void begin(boolean blend) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (blend) {
            // Vanilla crosshair blending: white pixels invert the background.
            GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
        } else {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static void end() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static void bind(Shape shape) {
        ResourceLocation texture = TEXTURES.get(shape);
        if (texture == null) texture = TEXTURES.get(Shape.CROSSHAIR);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    private static void drawSolid(StyleSetting style, float centerX, float centerY, float expansion, int argb, float alphaScale) {
        bind(style.shape);
        float red = ((argb >> 16) & 0xFF) / 255.0F;
        float green = ((argb >> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        float alpha = (((argb >>> 24) & 0xFF) / 255.0F) * alphaScale;
        GL11.glColor4f(red, green, blue, alpha);

        // Snap the quad to whole pixels: an odd 15px sprite on a fractional centre otherwise ends
        // up one pixel wider on one side.
        int size = Math.max(1, Math.round(15.0F * style.scale * expansion));
        float left = Math.round(centerX - size / 2.0F);
        float top = Math.round(centerY - size / 2.0F);
        float right = left + size;
        float bottom = top + size;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(left, bottom, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(right, bottom, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(right, top, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(left, top, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
    }

    /**
     * Pixel-stepped wind-up reveal. Instead of a growing square hole, the sprite is drawn row by
     * row with a crosshair-shaped hole: a diamond for diagonal sprites and a plus for straight
     * crosses. Each charge stage removes one pixel ring of the hole, so the arms visibly gain one
     * pixel per stage.
     */
    private static void drawSplit(StyleSetting style, float centerX, float centerY, float gap,
                                  int solidColour, Tint tint, long now) {
        int gapPx = Math.round(gap);
        boolean diagonal = style.shape == Shape.CROSS_OPEN_DIAGONAL
                || style.shape == Shape.CROSS_DIAGONAL_SMALL;
        boolean plus = style.shape == Shape.CROSSHAIR || style.shape == Shape.CROSS_OPEN;
        if (gapPx <= 0 || (!diagonal && !plus)) {
            if (tint != null && tint.flow) {
                drawFlow(style, centerX, centerY, tint, now);
            } else {
                drawSolid(style, centerX, centerY, 1.0F, solidColour, 1.0F);
            }
            return;
        }

        bind(style.shape);
        int size = Math.max(1, Math.round(15.0F * style.scale));
        float left = Math.round(centerX - size / 2.0F);
        float top = Math.round(centerY - size / 2.0F);
        float pixel = (float) size / 15.0F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int py = 0; py < 15; py++) {
            int dy = py - 7;
            int hideStart = -1;
            int hideEnd = -1;
            if (diagonal) {
                int rem = gapPx - Math.abs(dy);
                if (rem > 0) {
                    hideStart = Math.max(0, 7 - rem);
                    hideEnd = Math.min(15, 7 + rem + 1);
                }
            } else {
                if (Math.abs(dy) <= 1) {
                    hideStart = Math.max(0, 7 - gapPx);
                    hideEnd = Math.min(15, 7 + gapPx + 1);
                }
                if (Math.abs(dy) <= gapPx) {
                    hideStart = hideStart < 0 ? 6 : Math.min(hideStart, 6);
                    hideEnd = hideEnd < 0 ? 9 : Math.max(hideEnd, 9);
                }
            }

            if (hideStart < 0) {
                addRowSegment(tessellator, 0, 15, py, left, top, pixel, solidColour, tint, now, centerX, centerY);
            } else {
                if (hideStart > 0) {
                    addRowSegment(tessellator, 0, hideStart, py, left, top, pixel, solidColour, tint, now, centerX, centerY);
                }
                if (hideEnd < 15) {
                    addRowSegment(tessellator, hideEnd, 15, py, left, top, pixel, solidColour, tint, now, centerX, centerY);
                }
            }
        }
        tessellator.draw();
    }

    private static void addRowSegment(Tessellator tessellator, int x0, int x1, int py,
                                      float left, float top, float pixel,
                                      int solidColour, Tint tint, long now, float centerX, float centerY) {
        if (x1 <= x0) return;
        float dx0 = left + x0 * pixel;
        float dx1 = left + x1 * pixel;
        float dy0 = top + py * pixel;
        float dy1 = dy0 + pixel;
        int argb = solidColour;
        if (tint != null && tint.flow) {
            float sampleX = (dx0 + dx1) * 0.5F;
            float sampleY = (dy0 + dy1) * 0.5F;
            argb = tint.colourAt((sampleX - centerX) * tint.guiScale,
                    (centerY - sampleY) * tint.guiScale, now);
        }
        tessellator.setColorRGBA_F(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
        float u0 = x0 / 15.0F;
        float u1 = x1 / 15.0F;
        float v0 = py / 15.0F;
        float v1 = (py + 1) / 15.0F;
        tessellator.addVertexWithUV(dx0, dy1, 0.0D, u0, v1);
        tessellator.addVertexWithUV(dx1, dy1, 0.0D, u1, v1);
        tessellator.addVertexWithUV(dx1, dy0, 0.0D, u1, v0);
        tessellator.addVertexWithUV(dx0, dy0, 0.0D, u0, v0);
    }

    /** Splits the sprite into a grid so Crosshair-Selection's spatial flow gradient survives. */
    private static void drawFlow(StyleSetting style, float centerX, float centerY, Tint tint, long now) {
        bind(style.shape);
        int size = Math.max(1, Math.round(15.0F * style.scale));
        float left = Math.round(centerX - size / 2.0F);
        float top = Math.round(centerY - size / 2.0F);
        float step = (float) size / FLOW_CELLS;
        float[] rgba = new float[4];
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int cellX = 0; cellX < FLOW_CELLS; cellX++) {
            for (int cellY = 0; cellY < FLOW_CELLS; cellY++) {
                float x0 = left + cellX * step;
                float y0 = top + cellY * step;
                float x1 = x0 + step;
                float y1 = y0 + step;
                float sampleX = (x0 + x1) * 0.5F;
                float sampleY = (y0 + y1) * 0.5F;
                int argb = tint.colourAt((sampleX - centerX) * tint.guiScale,
                        (centerY - sampleY) * tint.guiScale, now);
                rgba[0] = ((argb >> 16) & 0xFF) / 255.0F;
                rgba[1] = ((argb >> 8) & 0xFF) / 255.0F;
                rgba[2] = (argb & 0xFF) / 255.0F;
                rgba[3] = ((argb >>> 24) & 0xFF) / 255.0F;
                tessellator.setColorRGBA_F(rgba[0], rgba[1], rgba[2], rgba[3]);
                float u0 = (float) cellX / FLOW_CELLS;
                float u1 = (float) (cellX + 1) / FLOW_CELLS;
                float v0 = (float) cellY / FLOW_CELLS;
                float v1 = (float) (cellY + 1) / FLOW_CELLS;
                tessellator.addVertexWithUV(x0, y0, 0.0D, u0, v0);
                tessellator.addVertexWithUV(x1, y0, 0.0D, u1, v0);
                tessellator.addVertexWithUV(x1, y1, 0.0D, u1, v1);
                tessellator.addVertexWithUV(x0, y1, 0.0D, u0, v1);
            }
        }
        tessellator.draw();
    }

    private static void drawDot(float centerX, float centerY, int argb, float size) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float red = ((argb >> 16) & 0xFF) / 255.0F;
        float green = ((argb >> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);

        float half = Math.max(0.5F, 7.5F * (size / 15.0F));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertex(centerX - half, centerY + half, 0.0D);
        tessellator.addVertex(centerX + half, centerY + half, 0.0D);
        tessellator.addVertex(centerX + half, centerY - half, 0.0D);
        tessellator.addVertex(centerX - half, centerY - half, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}
