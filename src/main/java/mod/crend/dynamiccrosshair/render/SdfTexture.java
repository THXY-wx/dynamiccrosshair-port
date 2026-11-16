package mod.crend.dynamiccrosshair.render;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import mod.crend.dynamiccrosshair.config.Shape;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Bakes each 15x15 pixel sprite into a signed distance field texture. Rendering the field with a
 * smoothstep gives the anti-aliased, vector-like look of Crosshair-Selection instead of blocky
 * pixel edges.
 */
final class SdfTexture {

    private static final int RESOLUTION = 64;
    private static final float RANGE = 8.0F;
    private static final Map<Shape, Integer> TEXTURES = new EnumMap<>(Shape.class);
    private static boolean failed;

    private SdfTexture() {}

    static boolean ensure() {
        if (!TEXTURES.isEmpty()) return true;
        if (failed) return false;
        try {
            for (Shape shape : Shape.values()) {
                Integer id = bake(shape);
                if (id != null) TEXTURES.put(shape, id);
            }
            return !TEXTURES.isEmpty();
        } catch (Throwable t) {
            failed = true;
            return false;
        }
    }

    static int texture(Shape shape) {
        Integer id = TEXTURES.get(shape);
        return id == null ? 0 : id;
    }

    private static Integer bake(Shape shape) throws Exception {
        ResourceLocation location = new ResourceLocation("dynamiccrosshair",
                "textures/gui/crosshair/" + shape.textureName + ".png");
        BufferedImage image;
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager()
                .getResource(location).getInputStream()) {
            image = ImageIO.read(stream);
        }
        if (image == null) return null;
        boolean[][] mask = new boolean[15][15];
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
                int argb = image.getRGB(Math.min(x, image.getWidth() - 1), Math.min(y, image.getHeight() - 1));
                mask[x][y] = ((argb >>> 24) & 0xFF) > 100;
            }
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(RESOLUTION * RESOLUTION);
        float scale = 15.0F / RESOLUTION;
        for (int gy = 0; gy < RESOLUTION; gy++) {
            for (int gx = 0; gx < RESOLUTION; gx++) {
                float px = (gx + 0.5F) * scale;
                float py = (gy + 0.5F) * scale;
                boolean inside = mask[Math.min(14, (int) px)][Math.min(14, (int) py)];
                float best = RANGE;
                for (int oy = 0; oy < 15; oy++) {
                    for (int ox = 0; ox < 15; ox++) {
                        if (mask[ox][oy] == inside) continue;
                        float dx = px - (ox + 0.5F);
                        float dy = py - (oy + 0.5F);
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);
                        if (distance < best) best = distance;
                    }
                }
                float signed = inside ? -best : best;
                float normalised = (signed / RANGE + 1.0F) * 0.5F;
                int value = (int) (normalised * 255.0F);
                buffer.put((byte) (value < 0 ? 0 : (value > 255 ? 255 : value)));
            }
        }
        buffer.flip();

        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_LUMINANCE, RESOLUTION, RESOLUTION, 0,
                GL11.GL_LUMINANCE, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        return id;
    }
}
