package mod.crend.dynamiccrosshair.render;

import mod.crend.dynamiccrosshair.bobbing.Bobbing;
import mod.crend.dynamiccrosshair.compat.CrosshairSelectionCompat;
import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.config.StyleSetting;
import mod.crend.dynamiccrosshair.crosshair.CrosshairHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Suppresses the vanilla crosshair in the HUD event and draws the real crosshair from
 * RenderTickEvent.END, exactly like Crosshair-Selection. Drawing outside the HUD event avoids
 * Angelica's HUD cache, which was resampling the crosshair (jaggies) and updating it at a lower
 * frame rate (stuttering bobbing/arrow).
 */
public class CrosshairOverlay {

    private static long lastWarning;

    private static void warnOnce(String message, Throwable throwable) {
        long now = System.currentTimeMillis();
        if (now - lastWarning < 10000L) return;
        lastWarning = now;
        mod.crend.dynamiccrosshair.DynamicCrosshair.LOGGER.error(message, throwable);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (!Config.enabled) return;
        if (CrosshairSelectionCompat.shouldYield()) return;
        // The actual drawing happens in renderFrame; just hide the vanilla crosshair here.
        event.setCanceled(true);
    }

    public static void renderFrame(Minecraft mc, float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.hideGUI) return;
        if (!Config.enabled) return;

        try {
            CrosshairHandler.update(mc);
        } catch (Throwable t) {
            warnOnce("Crosshair evaluation failed", t);
            return;
        }

        boolean drive = CrosshairSelectionCompat.shouldDrive();
        if (!drive && CrosshairSelectionCompat.shouldYield()) return;

        if (Config.disableDebugCrosshair && mc.gameSettings.showDebugInfo) return;
        if (!Config.thirdPersonCrosshair && mc.gameSettings.thirdPersonView != 0) return;
        if (Config.hideWithScreen && mc.currentScreen != null) return;
        if (Config.hideWithMap && CrosshairHandler.isHoldingMap(mc)) return;
        if (!CrosshairHandler.shouldShow()) return;

        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;
        int guiScale = Math.max(1, resolution.getScaleFactor());

        Bobbing.Transform transform;
        try {
            transform = Bobbing.compute(mc, partialTicks, width, height);
        } catch (Throwable t) {
            warnOnce("Crosshair bobbing failed; using the centred crosshair", t);
            transform = new Bobbing.Transform();
        }

        CrosshairSelectionCompat.Tint tint = null;
        if (drive && Config.adoptSelectionColours) {
            try {
                float centreFragY = mc.displayHeight - centerY * guiScale;
                tint = CrosshairSelectionCompat.readTint(guiScale, mc.displayHeight, centreFragY);
            } catch (Throwable t) {
                warnOnce("Could not read Crosshair-Selection colours", t);
            }
        }
        boolean vector = drive && Config.useSelectionRenderer && CrosshairSdfRenderer.isAvailable();
        java.nio.IntBuffer viewport = org.lwjgl.BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        int frameWidth = viewport.get(2);
        int frameHeight = viewport.get(3);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        try {
            GL11.glTranslatef(centerX + transform.dx, centerY + transform.dy, 0.0F);
            GL11.glRotatef(transform.rotation, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(transform.scaleX, transform.scaleY, 1.0F);
            GL11.glTranslatef(-centerX, -centerY, 0.0F);

            for (StyleSetting style : CrosshairHandler.baseStyles()) {
                if (vector) {
                    CrosshairSdfRenderer.draw(style, centerX, centerY, transform, tint, guiScale,
                            width, height, frameWidth, frameHeight);
                } else if (tint != null) {
                    CrosshairRenderer.drawTinted(style, centerX, centerY, tint, transform.gap);
                } else {
                    CrosshairRenderer.draw(style, centerX, centerY, transform.gap);
                }
            }
            for (StyleSetting modifier : CrosshairHandler.modifiers()) {
                if (vector) {
                    CrosshairSdfRenderer.draw(modifier, centerX, centerY, transform, null, guiScale,
                            width, height, frameWidth, frameHeight);
                } else {
                    CrosshairRenderer.drawModifier(modifier, centerX, centerY);
                }
            }
        } catch (Throwable t) {
            warnOnce("Crosshair rendering failed", t);
        } finally {
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopAttrib();
        }

        if (Config.crossbowIndicator && mc.thePlayer != null) {
            float indicatorX = centerX + transform.dx;
            float indicatorY = centerY + transform.dy;
            boolean handled = drive
                    && CrosshairSelectionCompat.drawBowIndicator(mc, indicatorX, indicatorY);
            if (!handled) {
                net.minecraft.item.ItemStack held = mc.thePlayer.getCurrentEquippedItem();
                if (mod.crend.dynamiccrosshair.compat.TinkersCompat.isChargedCrossbow(held, mc.thePlayer)) {
                    int colour = tint != null
                            ? tint.colourAt(0.0F, 0.0F, System.currentTimeMillis())
                            : Config.regular.color;
                    colour |= 0xFF000000;
                    mc.fontRenderer.drawStringWithShadow("!",
                            (int) (indicatorX + 12.0F), (int) (indicatorY - 4.0F), colour);
                }
            }
        }
    }
}
