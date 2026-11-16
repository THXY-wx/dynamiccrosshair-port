package mod.crend.dynamiccrosshair.crosshair;

import mod.crend.dynamiccrosshair.api.DynamicCrosshairApi;
import mod.crend.dynamiccrosshair.api.IInteractionProvider;
import mod.crend.dynamiccrosshair.api.InteractionType;
import mod.crend.dynamiccrosshair.compat.Compat;
import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.config.Policy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/** Turns a {@link CrosshairContext} into the crosshair roles that should be drawn. */
public final class CrosshairEvaluator {

    private CrosshairEvaluator() {}

    public static CrosshairState evaluate(Minecraft mc) {
        CrosshairState state = new CrosshairState();
        if (mc.thePlayer == null || mc.theWorld == null) return state;

        CrosshairContext context = new CrosshairContext(mc);

        if (context.withEntity) {
            if (Config.dynamicOnEntity) state.primary = CrosshairVariant.ON_ENTITY;
            InteractionType entityType = null;
            for (IInteractionProvider provider : DynamicCrosshairApi.providers()) {
                InteractionType candidate = provider.getEntityInteraction(context);
                if (candidate != null) { entityType = candidate; break; }
            }
            if (entityType == null) entityType = Compat.classifyEntity(context.entity);
            apply(state, entityType);
        } else if (context.withBlock) {
            if (Config.dynamicOnBlock) state.primary = CrosshairVariant.ON_BLOCK;
            if (Config.dynamicOnInteractableBlock && Compat.isInteractable(context)) {
                if (state.secondary == CrosshairVariant.NONE) state.secondary = CrosshairVariant.CAN_INTERACT;
            }
            for (IInteractionProvider provider : DynamicCrosshairApi.providers()) {
                InteractionType candidate = provider.getBlockInteraction(context);
                if (candidate != null) { apply(state, candidate); break; }
            }
        }

        InteractionType itemType = null;
        for (IInteractionProvider provider : DynamicCrosshairApi.providers()) {
            InteractionType candidate = provider.getItemInteraction(context);
            if (candidate != null) { itemType = candidate; break; }
        }
        if (itemType == null) itemType = Compat.classifyItem(context);
        if (itemType == InteractionType.PLACE_BLOCK
                && !Compat.isPolicyAllowed(Config.holdingBlock, Compat.canPlaceAt(context))) {
            itemType = InteractionType.EMPTY;
        }
        apply(state, itemType);

        if (Config.displayCorrectTool) {
            if (itemType == InteractionType.CORRECT_TOOL) state.correctTool = true;
            else if (itemType == InteractionType.INCORRECT_TOOL) state.incorrectTool = true;
        }

        if (state.primary != CrosshairVariant.NONE && state.secondary != CrosshairVariant.NONE) {
            if (!state.primary.style().coalesce) state.secondary = CrosshairVariant.NONE;
            else if (!state.secondary.style().coalesce) state.primary = CrosshairVariant.NONE;
        }
        // A held item's crosshair is exclusive: it must not be overlaid on the block/entity/interact
        // style. Either side can be the held style (TOOL sets primary, weapons set secondary).
        boolean primaryHolding = isHolding(state.primary);
        boolean secondaryHolding = isHolding(state.secondary);
        if (primaryHolding && secondaryHolding) {
            state.primary = CrosshairVariant.NONE;
        } else if (primaryHolding) {
            state.secondary = CrosshairVariant.NONE;
        } else if (secondaryHolding) {
            state.primary = CrosshairVariant.NONE;
        }
        return state;
    }

    private static boolean isHolding(CrosshairVariant variant) {
        return variant == CrosshairVariant.HOLDING_TOOL
                || variant == CrosshairVariant.HOLDING_MELEE
                || variant == CrosshairVariant.HOLDING_RANGED
                || variant == CrosshairVariant.HOLDING_THROWABLE
                || variant == CrosshairVariant.HOLDING_BLOCK
                || variant == CrosshairVariant.HOLDING_USABLE
                || variant == CrosshairVariant.HOLDING_SHIELD;
    }

    private static void apply(CrosshairState state, InteractionType type) {
        if (type == null) return;
        switch (type) {
            case TARGET_BLOCK:
                if (state.primary == CrosshairVariant.NONE) state.primary = CrosshairVariant.ON_BLOCK;
                break;
            case TARGET_ENTITY:
                if (state.primary == CrosshairVariant.NONE) state.primary = CrosshairVariant.ON_ENTITY;
                break;
            case PLACE_BLOCK:
                if (Compat.isPolicyAllowed(Config.holdingBlock, true)) state.secondary = CrosshairVariant.HOLDING_BLOCK;
                break;
            case MELEE_WEAPON:
                if (Config.holdingMeleeWeapon) state.secondary = CrosshairVariant.HOLDING_MELEE;
                break;
            case RANGED_WEAPON:
            case RANGED_WEAPON_CHARGING:
                if (Config.holdingRangedWeapon) state.secondary = CrosshairVariant.HOLDING_RANGED;
                break;
            case THROW_ITEM:
                if (Config.holdingThrowable) state.secondary = CrosshairVariant.HOLDING_THROWABLE;
                break;
            case SHIELD:
                if (Config.holdingShield) state.secondary = CrosshairVariant.HOLDING_SHIELD;
                break;
            case USE_ITEM:
            case EQUIP_ITEM:
            case CONSUME_ITEM:
            case CHARGE_ITEM:
            case USE_ITEM_ON_BLOCK:
            case USE_ITEM_ON_ENTITY:
            case PLACE_ITEM_ON_BLOCK:
            case TAKE_ITEM_FROM_BLOCK:
            case USE_BLOCK:
                if (Config.holdingUsableItem != Policy.DISABLED) state.secondary = CrosshairVariant.HOLDING_USABLE;
                break;
            case INTERACT_WITH_BLOCK:
            case INTERACT_WITH_ENTITY:
            case PICK_UP_ENTITY:
            case PICK_UP_BLOCK:
            case MOUNT_ENTITY:
            case MOUNT_BLOCK:
                state.secondary = CrosshairVariant.CAN_INTERACT;
                break;
            case TOOL:
            case CORRECT_TOOL:
            case INCORRECT_TOOL:
                if (Config.holdingTool) {
                    state.primary = CrosshairVariant.HOLDING_TOOL;
                    state.correctTool = type == InteractionType.CORRECT_TOOL;
                    state.incorrectTool = type == InteractionType.INCORRECT_TOOL;
                }
                break;
            case USABLE_TOOL:
                if (Config.holdingTool) state.primary = CrosshairVariant.HOLDING_TOOL;
                if (Config.holdingUsableItem != Policy.DISABLED) state.secondary = CrosshairVariant.HOLDING_USABLE;
                break;
            case FORCE_REGULAR_CROSSHAIR:
                state.primary = CrosshairVariant.REGULAR;
                break;
            default:
                break;
        }
    }
}
