package mod.crend.dynamiccrosshair.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mod.crend.dynamiccrosshair.DynamicCrosshair;
import mod.crend.dynamiccrosshair.config.Config;

/** Public entry point for other mods. */
public final class DynamicCrosshairApi {

    private static final List<IInteractionProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private DynamicCrosshairApi() {}

    public static void register(IInteractionProvider provider) {
        if (provider == null) return;
        PROVIDERS.add(provider);
        DynamicCrosshair.LOGGER.info("Registered interaction provider from mod {}", provider.getModId());
    }

    public static List<IInteractionProvider> providers() {
        return PROVIDERS;
    }

    /** Force a block (modid:name or modid:name:meta) to count as interactable. */
    public static void registerInteractableBlock(String blockId) {
        Config.addForcedBlock(blockId, true);
    }

    /** Force a block to never count as interactable. */
    public static void registerNonInteractableBlock(String blockId) {
        Config.addForcedBlock(blockId, false);
    }

    /** Force an item (modid:name or modid:name:meta) to count as usable. */
    public static void registerUsableItem(String itemId) {
        Config.addForcedItem(itemId, true);
    }

    /** Force an item to never count as usable. */
    public static void registerNonUsableItem(String itemId) {
        Config.addForcedItem(itemId, false);
    }
}
