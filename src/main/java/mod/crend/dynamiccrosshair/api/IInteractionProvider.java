package mod.crend.dynamiccrosshair.api;

import mod.crend.dynamiccrosshair.crosshair.CrosshairContext;

/**
 * Implement this and register it with {@link DynamicCrosshairApi#register(IInteractionProvider)} to
 * teach the crosshair about modded blocks, items and entities.
 */
public interface IInteractionProvider {

    /** Mod id this provider is responsible for, used for logging. */
    String getModId();

    /** Return a type to override block handling, or {@code null} to keep the default behaviour. */
    default InteractionType getBlockInteraction(CrosshairContext context) {
        return null;
    }

    /** Return a type to override item handling, or {@code null}. */
    default InteractionType getItemInteraction(CrosshairContext context) {
        return null;
    }

    /** Return a type to override entity handling, or {@code null}. */
    default InteractionType getEntityInteraction(CrosshairContext context) {
        return null;
    }
}
