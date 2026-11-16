package mod.crend.dynamiccrosshair.crosshair;

import java.util.ArrayList;
import java.util.List;

import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.config.StyleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

/** Per-frame state cache used by the renderer. */
public final class CrosshairHandler {

    private static CrosshairState state = new CrosshairState();
    private static boolean shouldShow = true;

    private CrosshairHandler() {}

    public static void update(Minecraft mc) {
        state = CrosshairEvaluator.evaluate(mc);
        shouldShow = shouldShowCrosshair(mc, state);
    }

    private static boolean shouldShowCrosshair(Minecraft mc, CrosshairState state) {
        if (!Config.enabled) return true;
        if (state.hasInteraction()) return true;
        return !Config.dynamicCrosshair;
    }

    public static CrosshairState getState() {
        return state;
    }

    public static boolean shouldShow() {
        return shouldShow;
    }

    public static List<StyleSetting> baseStyles() {
        List<StyleSetting> styles = new ArrayList<>(3);
        if (state.primary != CrosshairVariant.NONE) styles.add(state.primary.style());
        if (state.secondary != CrosshairVariant.NONE) styles.add(state.secondary.style());
        if (styles.isEmpty()) styles.add(Config.regular);
        return styles;
    }

    public static List<StyleSetting> modifiers() {
        List<StyleSetting> styles = new ArrayList<>(2);
        if (state.correctTool) styles.add(Config.modifierCorrect);
        if (state.incorrectTool) styles.add(Config.modifierIncorrect);
        return styles;
    }

    public static List<StyleSetting> styles() {
        List<StyleSetting> styles = baseStyles();
        styles.addAll(modifiers());
        return styles;
    }

    public static boolean isHoldingMap(Minecraft mc) {
        if (mc.thePlayer == null) return false;
        ItemStack held = mc.thePlayer.getCurrentEquippedItem();
        return held != null && held.getItem() instanceof ItemMap;
    }

    public static boolean isTargetingBlock(Minecraft mc) {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }
}
