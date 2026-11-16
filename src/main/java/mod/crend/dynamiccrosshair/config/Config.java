package mod.crend.dynamiccrosshair.config;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Configuration for the 1.7.10 port. Values are stored in config/dynamiccrosshair.cfg and can be
 * edited in game through the config GUI (F9 by default, or the mod list "Config" button).
 */
public final class Config {

    private static Configuration configuration;

    // ------------------------------------------------------------------ general
    public static boolean enabled = true;
    public static boolean dynamicCrosshair = true;
    public static boolean disableDebugCrosshair = false;
    public static boolean thirdPersonCrosshair = false;
    public static boolean hideWithScreen = true;
    public static boolean hideWithMap = true;

    // ------------------------------------------------------------------ dynamic rules
    public static boolean dynamicOnBlock = true;
    public static boolean dynamicOnInteractableBlock = true;
    public static boolean dynamicOnEntity = true;
    public static boolean displayCorrectTool = true;
    public static boolean holdingTool = true;
    public static boolean holdingMeleeWeapon = true;
    public static boolean holdingRangedWeapon = true;
    public static boolean holdingThrowable = true;
    public static boolean holdingShield = true;
    public static Policy holdingBlock = Policy.IF_INTERACTABLE;
    public static Policy holdingUsableItem = Policy.IF_INTERACTABLE;

    // ------------------------------------------------------------------ bobbing
    public static boolean bobbingEnabled = true;
    public static boolean bobView = true;
    public static boolean bobSneak = true;
    public static boolean bobItemSwitch = true;
    public static boolean bobAttack = true;
    public static boolean bobEat = true;
    public static boolean bobBow = true;
    public static boolean placeFeedback = true;
    public static boolean crossbowIndicator = true;
    public static float bobStrength = 1.0F;

    // ------------------------------------------------------------------ compat
    public static CompatMode crosshairSelection = CompatMode.AUTO;
    public static boolean adoptSelectionColours = true;
    public static boolean adoptSelectionOutline = false;
    public static boolean useSelectionRenderer = true;
    private static final Set<String> interactableBlocks = new HashSet<>();
    private static final Set<String> nonInteractableBlocks = new HashSet<>();
    private static final Set<String> usableItems = new HashSet<>();
    private static final Set<String> nonUsableItems = new HashSet<>();

    // ------------------------------------------------------------------ styles
    public static StyleSetting regular = new StyleSetting(Shape.CROSS_OPEN, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting onBlock = new StyleSetting(Shape.CROSS_OPEN, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting onEntity = new StyleSetting(Shape.CROSS_OPEN_DIAGONAL, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting holdingBlockStyle = new StyleSetting(Shape.DIAMOND, 0xFFFFFFFF, true, false, 1.0F);
    public static StyleSetting holdingToolStyle = new StyleSetting(Shape.SQUARE, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting holdingMeleeStyle = new StyleSetting(Shape.CROSS_OPEN, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting holdingRangedStyle = new StyleSetting(Shape.CROSS_OPEN_DIAGONAL, 0xFFFFFFFF, true, false, 1.0F);
    public static StyleSetting holdingThrowableStyle = new StyleSetting(Shape.CIRCLE_LARGE, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting holdingUsableStyle = new StyleSetting(Shape.BRACKETS_ROUND, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting interactStyle = new StyleSetting(Shape.BRACKETS, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting shieldStyle = new StyleSetting(Shape.BRACKETS_BOTTOM, 0xFFFFFFFF, true, true, 1.0F);
    public static StyleSetting modifierCorrect = new StyleSetting(Shape.DOT, 0xFF55FF55, true, true, 1.0F);
    public static StyleSetting modifierIncorrect = new StyleSetting(Shape.DOT, 0xFFFF5555, true, true, 1.0F);

    private Config() {}

    public static void load(File file) {
        configuration = new Configuration(file);
        configuration.load();

        configuration.addCustomCategoryComment("general", "General behaviour");
        enabled = configuration.getBoolean("enabled", "general", enabled, "Master switch. false restores the vanilla crosshair.");
        dynamicCrosshair = configuration.getBoolean("dynamicCrosshair", "general", dynamicCrosshair, "Hide the crosshair when there is no interaction.");
        disableDebugCrosshair = configuration.getBoolean("disableDebugCrosshair", "general", disableDebugCrosshair, "Hide the crosshair while the F3 debug screen is open.");
        thirdPersonCrosshair = configuration.getBoolean("thirdPersonCrosshair", "general", thirdPersonCrosshair, "Show the crosshair in third person.");
        hideWithScreen = configuration.getBoolean("hideWithScreen", "general", hideWithScreen, "Hide the crosshair while a GUI screen is open.");
        hideWithMap = configuration.getBoolean("hideWithMap", "general", hideWithMap, "Hide the crosshair while holding a filled map.");

        configuration.addCustomCategoryComment("dynamic", "Which contexts change the crosshair");
        dynamicOnBlock = configuration.getBoolean("onBlock", "dynamic", dynamicOnBlock, "Show a crosshair when targeting a block.");
        dynamicOnInteractableBlock = configuration.getBoolean("onInteractableBlock", "dynamic", dynamicOnInteractableBlock, "Show a crosshair when targeting an interactable block.");
        dynamicOnEntity = configuration.getBoolean("onEntity", "dynamic", dynamicOnEntity, "Show a crosshair when targeting an entity.");
        displayCorrectTool = configuration.getBoolean("displayCorrectTool", "dynamic", displayCorrectTool, "Show green/red dots for correct/incorrect tools.");
        holdingTool = configuration.getBoolean("holdingTool", "dynamic", holdingTool, "Crosshair while holding a tool.");
        holdingMeleeWeapon = configuration.getBoolean("holdingMeleeWeapon", "dynamic", holdingMeleeWeapon, "Crosshair while holding a melee weapon.");
        holdingRangedWeapon = configuration.getBoolean("holdingRangedWeapon", "dynamic", holdingRangedWeapon, "Crosshair while holding a ranged weapon.");
        holdingThrowable = configuration.getBoolean("holdingThrowable", "dynamic", holdingThrowable, "Crosshair while holding a throwable item.");
        holdingShield = configuration.getBoolean("holdingShield", "dynamic", holdingShield, "Crosshair while holding a shield (kept for compatibility).");
        holdingBlock = Policy.byName(configuration.getString("holdingBlock", "dynamic", holdingBlock.name(), "ALWAYS, IF_INTERACTABLE or DISABLED"));
        holdingUsableItem = Policy.byName(configuration.getString("holdingUsableItem", "dynamic", holdingUsableItem.name(), "ALWAYS, IF_INTERACTABLE or DISABLED"));

        configuration.addCustomCategoryComment("bobbing", "Crosshair bobbing (CrosshairBobbingMod behaviour)");
        bobbingEnabled = configuration.getBoolean("enabled", "bobbing", bobbingEnabled, "Master switch for crosshair bobbing.");
        bobView = configuration.getBoolean("viewBobbing", "bobbing", bobView, "Follow the vanilla view bobbing.");
        bobSneak = configuration.getBoolean("sneak", "bobbing", bobSneak, "Rotate while sneaking.");
        bobItemSwitch = configuration.getBoolean("itemSwitch", "bobbing", bobItemSwitch, "Shift while switching items.");
        bobAttack = configuration.getBoolean("attack", "bobbing", bobAttack, "Kick and scale on attack.");
        bobEat = configuration.getBoolean("eat", "bobbing", bobEat, "Rotate while eating.");
        bobBow = configuration.getBoolean("bow", "bobbing", bobBow, "Scale while drawing a bow.");
        placeFeedback = configuration.getBoolean("placeFeedback", "bobbing", placeFeedback, "Shake the crosshair when a block is placed.");
        crossbowIndicator = configuration.getBoolean("crossbowIndicator", "bobbing", crossbowIndicator,
                "Use Crosshair-Selection's bow/crossbow charge indicator (arrow/percentage); falls back to '!' when it is absent.");
        bobStrength = configuration.getFloat("strength", "bobbing", bobStrength, 0.0F, 3.0F, "Strength multiplier for all bobbing effects.");

        configuration.addCustomCategoryComment("compat", "Compatibility with other crosshair mods");
        crosshairSelection = CompatMode.byName(configuration.getString("crosshairSelection", "compat", crosshairSelection.name(), "AUTO, YIELD or FORCE_OURS"));
        adoptSelectionColours = configuration.getBoolean("adoptSelectionColours", "compat", adoptSelectionColours,
                "AUTO mode: reuse Crosshair-Selection's colour and flowing rainbow for the dynamic styles.");
        adoptSelectionOutline = configuration.getBoolean("adoptSelectionOutline", "compat", adoptSelectionOutline,
                "AUTO mode: also draw Crosshair-Selection's outline around the pixel sprites (usually ugly on thin sprites).");
        useSelectionRenderer = configuration.getBoolean("useSelectionRenderer", "compat", useSelectionRenderer,
                "AUTO mode: render the dynamic shapes through the smooth Crosshair-Selection-style SDF shader instead of pixel sprites.");
        readSet("interactableBlocks", "Blocks that always count as interactable (modid:name or modid:name:meta), comma separated.");
        readSet("nonInteractableBlocks", "Blocks that never count as interactable, comma separated.");
        readSet("usableItems", "Items that always count as usable, comma separated.");
        readSet("nonUsableItems", "Items that never count as usable, comma separated.");

        configuration.addCustomCategoryComment("style", "Crosshair sprites: shape:argb:blend:coalesce:scale");
        regular = style("regular", regular);
        onBlock = style("onBlock", onBlock);
        onEntity = style("onEntity", onEntity);
        holdingBlockStyle = style("holdingBlock", holdingBlockStyle);
        holdingToolStyle = style("holdingTool", holdingToolStyle);
        holdingMeleeStyle = style("holdingMeleeWeapon", holdingMeleeStyle);
        holdingRangedStyle = style("holdingRangedWeapon", holdingRangedStyle);
        holdingThrowableStyle = style("holdingThrowable", holdingThrowableStyle);
        holdingUsableStyle = style("holdingUsableItem", holdingUsableStyle);
        interactStyle = style("interact", interactStyle);
        shieldStyle = style("shield", shieldStyle);
        modifierCorrect = style("modifierCorrect", modifierCorrect);
        modifierIncorrect = style("modifierIncorrect", modifierIncorrect);

        configuration.save();
    }

    private static void readSet(String key, String comment) {
        String value = configuration.getString(key, "compat", "", comment);
        Set<String> target = key.startsWith("non") ? (key.contains("Block") ? nonInteractableBlocks : nonUsableItems)
                : (key.contains("Block") ? interactableBlocks : usableItems);
        target.clear();
        for (String entry : value.split(",")) {
            String trimmed = entry.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) target.add(trimmed);
        }
    }

    private static StyleSetting style(String key, StyleSetting fallback) {
        Property property = configuration.get("style", key, fallback.serialize(), "shape:argb:blend:coalesce:scale");
        StyleSetting parsed = StyleSetting.parse(property.getString(), fallback);
        if (!parsed.serialize().equals(property.getString())) property.set(parsed.serialize());
        return parsed;
    }

    public static void save() {
        if (configuration == null) return;
        configuration.get("general", "enabled", true).set(enabled);
        configuration.get("general", "dynamicCrosshair", true).set(dynamicCrosshair);
        configuration.get("general", "disableDebugCrosshair", true).set(disableDebugCrosshair);
        configuration.get("general", "thirdPersonCrosshair", true).set(thirdPersonCrosshair);
        configuration.get("general", "hideWithScreen", true).set(hideWithScreen);
        configuration.get("general", "hideWithMap", true).set(hideWithMap);
        configuration.get("dynamic", "onBlock", true).set(dynamicOnBlock);
        configuration.get("dynamic", "onInteractableBlock", true).set(dynamicOnInteractableBlock);
        configuration.get("dynamic", "onEntity", true).set(dynamicOnEntity);
        configuration.get("dynamic", "displayCorrectTool", true).set(displayCorrectTool);
        configuration.get("dynamic", "holdingTool", true).set(holdingTool);
        configuration.get("dynamic", "holdingMeleeWeapon", true).set(holdingMeleeWeapon);
        configuration.get("dynamic", "holdingRangedWeapon", true).set(holdingRangedWeapon);
        configuration.get("dynamic", "holdingThrowable", true).set(holdingThrowable);
        configuration.get("dynamic", "holdingShield", true).set(holdingShield);
        configuration.get("dynamic", "holdingBlock", "IF_INTERACTABLE").set(holdingBlock.name());
        configuration.get("dynamic", "holdingUsableItem", "IF_INTERACTABLE").set(holdingUsableItem.name());
        configuration.get("bobbing", "enabled", true).set(bobbingEnabled);
        configuration.get("bobbing", "viewBobbing", true).set(bobView);
        configuration.get("bobbing", "sneak", true).set(bobSneak);
        configuration.get("bobbing", "itemSwitch", true).set(bobItemSwitch);
        configuration.get("bobbing", "attack", true).set(bobAttack);
        configuration.get("bobbing", "eat", true).set(bobEat);
        configuration.get("bobbing", "bow", true).set(bobBow);
        configuration.get("bobbing", "placeFeedback", true).set(placeFeedback);
        configuration.get("bobbing", "crossbowIndicator", true).set(crossbowIndicator);
        configuration.get("bobbing", "strength", 1.0F).set(bobStrength);
        configuration.get("compat", "crosshairSelection", "AUTO").set(crosshairSelection.name());
        configuration.get("compat", "adoptSelectionColours", true).set(adoptSelectionColours);
        configuration.get("compat", "adoptSelectionOutline", false).set(adoptSelectionOutline);
        configuration.get("compat", "useSelectionRenderer", true).set(useSelectionRenderer);
        configuration.get("compat", "interactableBlocks", "").set(join(interactableBlocks));
        configuration.get("compat", "nonInteractableBlocks", "").set(join(nonInteractableBlocks));
        configuration.get("compat", "usableItems", "").set(join(usableItems));
        configuration.get("compat", "nonUsableItems", "").set(join(nonUsableItems));
        configuration.get("style", "regular", "").set(regular.serialize());
        configuration.get("style", "onBlock", "").set(onBlock.serialize());
        configuration.get("style", "onEntity", "").set(onEntity.serialize());
        configuration.get("style", "holdingBlock", "").set(holdingBlockStyle.serialize());
        configuration.get("style", "holdingTool", "").set(holdingToolStyle.serialize());
        configuration.get("style", "holdingMeleeWeapon", "").set(holdingMeleeStyle.serialize());
        configuration.get("style", "holdingRangedWeapon", "").set(holdingRangedStyle.serialize());
        configuration.get("style", "holdingThrowable", "").set(holdingThrowableStyle.serialize());
        configuration.get("style", "holdingUsableItem", "").set(holdingUsableStyle.serialize());
        configuration.get("style", "interact", "").set(interactStyle.serialize());
        configuration.get("style", "shield", "").set(shieldStyle.serialize());
        configuration.get("style", "modifierCorrect", "").set(modifierCorrect.serialize());
        configuration.get("style", "modifierIncorrect", "").set(modifierIncorrect.serialize());
        configuration.save();
    }

    public static void resetDefaults() {
        enabled = true;
        dynamicCrosshair = true;
        disableDebugCrosshair = false;
        thirdPersonCrosshair = false;
        hideWithScreen = true;
        hideWithMap = true;
        dynamicOnBlock = true;
        dynamicOnInteractableBlock = true;
        dynamicOnEntity = true;
        displayCorrectTool = true;
        holdingTool = true;
        holdingMeleeWeapon = true;
        holdingRangedWeapon = true;
        holdingThrowable = true;
        holdingShield = true;
        holdingBlock = Policy.IF_INTERACTABLE;
        holdingUsableItem = Policy.IF_INTERACTABLE;
        bobbingEnabled = true;
        bobView = true;
        bobSneak = true;
        bobItemSwitch = true;
        bobAttack = true;
        bobEat = true;
        bobBow = true;
        placeFeedback = true;
        crossbowIndicator = true;
        bobStrength = 1.0F;
        crosshairSelection = CompatMode.AUTO;
        adoptSelectionColours = true;
        adoptSelectionOutline = false;
        useSelectionRenderer = true;
        regular = new StyleSetting(Shape.CROSS_OPEN, 0xFFFFFFFF, true, true, 1.0F);
        onBlock = new StyleSetting(Shape.CROSS_OPEN, 0xFFFFFFFF, true, true, 1.0F);
        onEntity = new StyleSetting(Shape.CROSS_OPEN_DIAGONAL, 0xFFFFFFFF, true, true, 1.0F);
        holdingBlockStyle = new StyleSetting(Shape.DIAMOND, 0xFFFFFFFF, true, false, 1.0F);
        holdingToolStyle = new StyleSetting(Shape.SQUARE, 0xFFFFFFFF, true, true, 1.0F);
        holdingMeleeStyle = new StyleSetting(Shape.CROSS_OPEN, 0xFFFFFFFF, true, true, 1.0F);
        holdingRangedStyle = new StyleSetting(Shape.CROSS_OPEN_DIAGONAL, 0xFFFFFFFF, true, false, 1.0F);
        holdingThrowableStyle = new StyleSetting(Shape.CIRCLE_LARGE, 0xFFFFFFFF, true, true, 1.0F);
        holdingUsableStyle = new StyleSetting(Shape.BRACKETS_ROUND, 0xFFFFFFFF, true, true, 1.0F);
        interactStyle = new StyleSetting(Shape.BRACKETS, 0xFFFFFFFF, true, true, 1.0F);
        shieldStyle = new StyleSetting(Shape.BRACKETS_BOTTOM, 0xFFFFFFFF, true, true, 1.0F);
        modifierCorrect = new StyleSetting(Shape.DOT, 0xFF55FF55, true, true, 1.0F);
        modifierIncorrect = new StyleSetting(Shape.DOT, 0xFFFF5555, true, true, 1.0F);
        save();
    }

    public static boolean isBlockForcedInteractable(String id, int meta) {
        return matches(interactableBlocks, id, meta);
    }

    public static boolean isBlockForcedNonInteractable(String id, int meta) {
        return matches(nonInteractableBlocks, id, meta);
    }

    public static boolean isItemForcedUsable(String id, int meta) {
        return matches(usableItems, id, meta);
    }

    public static boolean isItemForcedNonUsable(String id, int meta) {
        return matches(nonUsableItems, id, meta);
    }

    public static void addForcedBlock(String id, boolean interactable) {
        if (id == null) return;
        String normalised = id.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) return;
        (interactable ? interactableBlocks : nonInteractableBlocks).add(normalised);
    }

    public static void addForcedItem(String id, boolean usable) {
        if (id == null) return;
        String normalised = id.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) return;
        (usable ? usableItems : nonUsableItems).add(normalised);
    }

    private static String join(Set<String> set) {
        StringBuilder builder = new StringBuilder();
        for (String entry : set) {
            if (builder.length() > 0) builder.append(',');
            builder.append(entry);
        }
        return builder.toString();
    }

    private static boolean matches(Set<String> set, String id, int meta) {
        if (id == null || set.isEmpty()) return false;
        String full = id.toLowerCase(Locale.ROOT) + ":" + meta;
        return set.contains(id.toLowerCase(Locale.ROOT)) || set.contains(full);
    }

    public static String dumpOverrides() {
        return "interactable=" + interactableBlocks + ", nonInteractable=" + nonInteractableBlocks
                + ", usable=" + usableItems + ", nonUsable=" + nonUsableItems;
    }

    public static Configuration raw() {
        return configuration;
    }

    public static java.util.List<String> allKeys() {
        return Arrays.asList("general", "dynamic", "bobbing", "compat", "style");
    }
}
