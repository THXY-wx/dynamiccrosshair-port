package mod.crend.dynamiccrosshair.api;

/**
 * Interaction classification, modelled after the DynamicCrosshair API. Mods may return these from
 * an {@link IInteractionProvider} to override the built-in heuristics.
 */
public enum InteractionType {

    USE_ITEM(InteractionMode.SECONDARY),
    EQUIP_ITEM(InteractionMode.SECONDARY),
    CONSUME_ITEM(InteractionMode.SECONDARY),
    THROW_ITEM(InteractionMode.SECONDARY),
    CHARGE_ITEM(InteractionMode.SECONDARY),
    PICK_UP_ITEM(InteractionMode.SECONDARY),

    TARGET_BLOCK(InteractionMode.PRIMARY),
    PLACE_BLOCK(InteractionMode.SECONDARY),
    INTERACT_WITH_BLOCK(InteractionMode.SECONDARY),
    PICK_UP_BLOCK(InteractionMode.SECONDARY),
    USE_ITEM_ON_BLOCK(InteractionMode.SECONDARY),
    PLACE_ITEM_ON_BLOCK(InteractionMode.SECONDARY),
    TAKE_ITEM_FROM_BLOCK(InteractionMode.SECONDARY),
    USE_BLOCK(InteractionMode.SECONDARY),
    MOUNT_BLOCK(InteractionMode.SECONDARY),

    TARGET_ENTITY(InteractionMode.PRIMARY),
    PLACE_ENTITY(InteractionMode.SECONDARY),
    INTERACT_WITH_ENTITY(InteractionMode.SECONDARY),
    PICK_UP_ENTITY(InteractionMode.SECONDARY),
    USE_ITEM_ON_ENTITY(InteractionMode.SECONDARY),
    MOUNT_ENTITY(InteractionMode.SECONDARY),

    MELEE_WEAPON(InteractionMode.SECONDARY),
    RANGED_WEAPON(InteractionMode.SECONDARY),
    RANGED_WEAPON_CHARGING(InteractionMode.SECONDARY),
    USABLE_TOOL(InteractionMode.BOTH),
    TOOL(InteractionMode.PRIMARY),
    CORRECT_TOOL(InteractionMode.PRIMARY),
    INCORRECT_TOOL(InteractionMode.PRIMARY),
    SHIELD(InteractionMode.SECONDARY),

    FORCE_REGULAR_CROSSHAIR(InteractionMode.BOTH),
    EMPTY(InteractionMode.NONE),
    NO_ACTION(InteractionMode.NONE);

    public final InteractionMode interactionMode;

    InteractionType(InteractionMode interactionMode) {
        this.interactionMode = interactionMode;
    }
}
