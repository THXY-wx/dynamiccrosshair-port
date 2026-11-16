package mod.crend.dynamiccrosshair.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** In-game configuration screen: tabs for general, dynamic, styles and bobbing settings. */
public class GuiDynamicCrosshairConfig extends GuiScreen {

    private static final String[] TABS = {"General", "Dynamic", "Styles", "Bobbing", "Compat"};
    private static final int[] COLOURS = {
            0xFFFFFFFF, 0xFFFF5555, 0xFF55FF55, 0xFF5555FF, 0xFFFFFF55,
            0xFF55FFFF, 0xFFFFAA00, 0xFFDD55FF, 0xFFAAAAAA, 0xFF000000
    };
    private static final float[] SCALES = {0.5F, 0.75F, 1.0F, 1.25F, 1.5F, 2.0F};

    private final GuiScreen parent;
    private int page;
    private int nextId;
    private final List<GuiRow> rows = new ArrayList<>();

    public GuiDynamicCrosshairConfig(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buildPage();
    }

    private void buildPage() {
        buttonList.clear();
        rows.clear();
        nextId = 1000;
        int tabWidth = 58;
        int tabsWidth = TABS.length * (tabWidth + 2);
        int tabX = width / 2 - tabsWidth / 2;
        for (int i = 0; i < TABS.length; i++) {
            GuiButton tab = new GuiButton(100 + i, tabX + i * (tabWidth + 2), 24, tabWidth, 18, Lang.t(TABS[i]));
            tab.enabled = i != page;
            buttonList.add(tab);
        }

        int left = width / 2 - 155;
        int right = width / 2 + 5;
        int top = 50;
        int step = 22;

        switch (page) {
            case 0:
                addRow(left, top + 0 * step, "Mod enabled", () -> onOff(Config.enabled), () -> Config.enabled = !Config.enabled);
                addRow(left, top + 1 * step, "Dynamic crosshair", () -> onOff(Config.dynamicCrosshair), () -> Config.dynamicCrosshair = !Config.dynamicCrosshair);
                addRow(left, top + 2 * step, "Hide with screen", () -> onOff(Config.hideWithScreen), () -> Config.hideWithScreen = !Config.hideWithScreen);
                addRow(left, top + 3 * step, "Hide with map", () -> onOff(Config.hideWithMap), () -> Config.hideWithMap = !Config.hideWithMap);
                addRow(left, top + 4 * step, "Third person", () -> onOff(Config.thirdPersonCrosshair), () -> Config.thirdPersonCrosshair = !Config.thirdPersonCrosshair);
                addRow(left, top + 5 * step, "Hide on F3", () -> onOff(Config.disableDebugCrosshair), () -> Config.disableDebugCrosshair = !Config.disableDebugCrosshair);
                addRow(left, top + 6 * step, "Correct tool dots", () -> onOff(Config.displayCorrectTool), () -> Config.displayCorrectTool = !Config.displayCorrectTool);
                addRow(right, top + 0 * step, "Bobbing", () -> onOff(Config.bobbingEnabled), () -> Config.bobbingEnabled = !Config.bobbingEnabled);
                addRow(right, top + 1 * step, "Bob strength", () -> String.format("%.2f", Config.bobStrength), () -> Config.bobStrength = cycle(Config.bobStrength, 0.0F, 3.0F, 0.25F));
                addRow(right, top + 2 * step, "Reset defaults", () -> "§c" + Lang.t("Reset"), () -> { Config.resetDefaults(); buildPage(); });
                addRow(right, top + 4 * step, "Done", () -> "§a" + Lang.t("Save & close"), this::done);
                break;
            case 1:
                addRow(left, top + 0 * step, "On block", () -> onOff(Config.dynamicOnBlock), () -> Config.dynamicOnBlock = !Config.dynamicOnBlock);
                addRow(left, top + 1 * step, "Interactable block", () -> onOff(Config.dynamicOnInteractableBlock), () -> Config.dynamicOnInteractableBlock = !Config.dynamicOnInteractableBlock);
                addRow(left, top + 2 * step, "On entity", () -> onOff(Config.dynamicOnEntity), () -> Config.dynamicOnEntity = !Config.dynamicOnEntity);
                addRow(left, top + 3 * step, "Holding tool", () -> onOff(Config.holdingTool), () -> Config.holdingTool = !Config.holdingTool);
                addRow(left, top + 4 * step, "Holding melee", () -> onOff(Config.holdingMeleeWeapon), () -> Config.holdingMeleeWeapon = !Config.holdingMeleeWeapon);
                addRow(left, top + 5 * step, "Holding ranged", () -> onOff(Config.holdingRangedWeapon), () -> Config.holdingRangedWeapon = !Config.holdingRangedWeapon);
                addRow(right, top + 0 * step, "Holding throwable", () -> onOff(Config.holdingThrowable), () -> Config.holdingThrowable = !Config.holdingThrowable);
                addRow(right, top + 1 * step, "Holding block", () -> Config.holdingBlock.name(), () -> Config.holdingBlock = cycle(Config.holdingBlock));
                addRow(right, top + 2 * step, "Holding usable", () -> Config.holdingUsableItem.name(), () -> Config.holdingUsableItem = cycle(Config.holdingUsableItem));
                break;
            case 2:
                styleRow(left, top + 0 * step, "Regular", () -> Config.regular);
                styleRow(left, top + 1 * step, "On block", () -> Config.onBlock);
                styleRow(left, top + 2 * step, "On entity", () -> Config.onEntity);
                styleRow(left, top + 3 * step, "Holding block", () -> Config.holdingBlockStyle);
                styleRow(left, top + 4 * step, "Holding tool", () -> Config.holdingToolStyle);
                styleRow(left, top + 5 * step, "Holding melee", () -> Config.holdingMeleeStyle);
                styleRow(right, top + 0 * step, "Holding ranged", () -> Config.holdingRangedStyle);
                styleRow(right, top + 1 * step, "Holding throwable", () -> Config.holdingThrowableStyle);
                styleRow(right, top + 2 * step, "Holding usable", () -> Config.holdingUsableStyle);
                styleRow(right, top + 3 * step, "Interact", () -> Config.interactStyle);
                styleRow(right, top + 4 * step, "Correct tool", () -> Config.modifierCorrect);
                styleRow(right, top + 5 * step, "Wrong tool", () -> Config.modifierIncorrect);
                break;
            case 3:
                addRow(left, top + 0 * step, "Bobbing", () -> onOff(Config.bobbingEnabled), () -> Config.bobbingEnabled = !Config.bobbingEnabled);
                addRow(left, top + 1 * step, "View bobbing", () -> onOff(Config.bobView), () -> Config.bobView = !Config.bobView);
                addRow(left, top + 2 * step, "Sneak", () -> onOff(Config.bobSneak), () -> Config.bobSneak = !Config.bobSneak);
                addRow(left, top + 3 * step, "Item switch", () -> onOff(Config.bobItemSwitch), () -> Config.bobItemSwitch = !Config.bobItemSwitch);
                addRow(left, top + 4 * step, "Place shake", () -> onOff(Config.placeFeedback), () -> Config.placeFeedback = !Config.placeFeedback);
                addRow(right, top + 0 * step, "Attack", () -> onOff(Config.bobAttack), () -> Config.bobAttack = !Config.bobAttack);
                addRow(right, top + 1 * step, "Eat", () -> onOff(Config.bobEat), () -> Config.bobEat = !Config.bobEat);
                addRow(right, top + 2 * step, "Bow", () -> onOff(Config.bobBow), () -> Config.bobBow = !Config.bobBow);
                addRow(right, top + 3 * step, "Bow indicator", () -> onOff(Config.crossbowIndicator), () -> Config.crossbowIndicator = !Config.crossbowIndicator);
                addRow(right, top + 3 * step, "Strength", () -> String.format("%.2f", Config.bobStrength), () -> Config.bobStrength = cycle(Config.bobStrength, 0.0F, 3.0F, 0.25F));
                break;
            case 4:
            default:
                addRow(left, top + 0 * step, "Crosshair-Selection", () -> Config.crosshairSelection.name(), () -> Config.crosshairSelection = cycle(Config.crosshairSelection));
                addRow(left, top + 1 * step, "Adopt colour/flow", () -> onOff(Config.adoptSelectionColours), () -> Config.adoptSelectionColours = !Config.adoptSelectionColours);
                addRow(left, top + 2 * step, "Adopt outline", () -> onOff(Config.adoptSelectionOutline), () -> Config.adoptSelectionOutline = !Config.adoptSelectionOutline);
                addRow(left, top + 3 * step, "Vector renderer", () -> onOff(Config.useSelectionRenderer), () -> Config.useSelectionRenderer = !Config.useSelectionRenderer);
                addRow(left, top + 4 * step, "Block overrides", () -> "", () -> {});
                addRow(left, top + 5 * step, "Item overrides", () -> "", () -> {});
                rows.get(rows.size() - 2).readOnly = true;
                rows.get(rows.size() - 1).readOnly = true;
                addRow(right, top + 0 * step, "Save config", () -> "§aSave", Config::save);
                break;
        }

        GuiButton done = new GuiButton(200, width / 2 - 100, height - 28, 200, 20, Lang.t("Done"));
        buttonList.add(done);
    }

    private void addRow(int x, int y, String label, Supplier<String> value, Runnable action) {
        if (y + 20 > height - 32) return;
        GuiRow row = new GuiRow(nextId++, x, y, 150, 18, Lang.t(label), value, action);
        rows.add(row);
        buttonList.add(row);
    }

    private void styleRow(int x, int y, String label, Supplier<StyleSetting> supplier) {
        if (y + 20 > height - 32) return;
        GuiRow shape = new GuiRow(nextId++, x, y, 108, 18, Lang.t(label), () -> supplier.get().shape.getId(), () -> {
            StyleSetting setting = supplier.get();
            Shape[] values = Shape.values();
            setting.shape = values[(setting.shape.ordinal() + 1) % values.length];
        });
        GuiRow colour = new GuiRow(nextId++, x + 110, y, 20, 18, "", () -> "", () -> {
            StyleSetting setting = supplier.get();
            int index = 0;
            for (int i = 0; i < COLOURS.length; i++) if (COLOURS[i] == setting.color) index = i;
            setting.color = COLOURS[(index + 1) % COLOURS.length];
        });
        colour.colourSwatch = () -> supplier.get().color;
        GuiRow scale = new GuiRow(nextId++, x + 132, y, 26, 18, "", () -> String.format("%.2f", supplier.get().scale), () -> {
            StyleSetting setting = supplier.get();
            int index = 0;
            for (int i = 0; i < SCALES.length; i++) if (Math.abs(SCALES[i] - setting.scale) < 0.001F) index = i;
            setting.scale = SCALES[(index + 1) % SCALES.length];
        });
        rows.add(shape);
        rows.add(colour);
        rows.add(scale);
        buttonList.add(shape);
        buttonList.add(colour);
        buttonList.add(scale);
    }

    private static String onOff(boolean value) {
        return value ? "§a" + Lang.t("ON") : "§c" + Lang.t("OFF");
    }

    private static float cycle(float value, float min, float max, float step) {
        float next = value + step;
        if (next > max + 0.001F) next = min;
        return next;
    }

    private static Policy cycle(Policy value) {
        Policy[] values = Policy.values();
        return values[(value.ordinal() + 1) % values.length];
    }

    private static CompatMode cycle(CompatMode value) {
        CompatMode[] values = CompatMode.values();
        return values[(value.ordinal() + 1) % values.length];
    }

    private void done() {
        Config.save();
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 200) {
            done();
            return;
        }
        if (button.id >= 100 && button.id < 100 + TABS.length) {
            page = button.id - 100;
            buildPage();
            return;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, Lang.t("Dynamic Crosshair"), width / 2, 8, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (page == 4) {
            String state = modLoaded() ? "§a" + Lang.t("Crosshair-Selection detected")
                    : "§7" + Lang.t("Crosshair-Selection not installed");
            drawCenteredString(fontRendererObj, state, width / 2, height - 44, 0xFFFFFF);
            drawCenteredString(fontRendererObj, Lang.t("AUTO yields when it draws, FORCE_OURS draws ours"), width / 2, height - 58, 0xAAAAAA);
        }
    }

    private static boolean modLoaded() {
        return cpw.mods.fml.common.Loader.isModLoaded("crosshairselection");
    }

    @Override
    public void onGuiClosed() {
        Config.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** A flat, clickable settings row with a label on the left and a value on the right. */
    public static class GuiRow extends GuiButton {

        private final String label;
        private final Supplier<String> value;
        private final Runnable action;
        public boolean readOnly;
        public Supplier<Integer> colourSwatch;

        public GuiRow(int id, int x, int y, int width, int height, String label, Supplier<String> value, Runnable action) {
            super(id, x, y, width, height, label);
            this.label = label;
            this.value = value;
            this.action = action;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hovered = mouseX >= xPosition && mouseY >= yPosition
                    && mouseX < xPosition + width && mouseY < yPosition + height;
            int background = readOnly ? 0x44222222 : (hovered ? 0xAA2A5A2A : 0x66101010);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, background);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + 1, 0xFF555555);
            drawRect(xPosition, yPosition + height - 1, xPosition + width, yPosition + height, 0xFF555555);
            int textY = yPosition + (height - 8) / 2;
            if (colourSwatch != null) {
                int colour = colourSwatch.get();
                drawRect(xPosition + 2, yPosition + 2, xPosition + width - 2, yPosition + height - 2, colour | 0xFF000000);
            } else if (!label.isEmpty()) {
                mc.fontRenderer.drawString(label, xPosition + 3, textY, 0xFFFFFF);
            }
            String text = value.get();
            if (text != null && !text.isEmpty()) {
                mc.fontRenderer.drawString(text, xPosition + width - 3 - mc.fontRenderer.getStringWidth(text), textY, 0xFFFF55);
            }
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            if (!super.mousePressed(mc, mouseX, mouseY)) return false;
            if (!readOnly && action != null) action.run();
            return true;
        }
    }
}
