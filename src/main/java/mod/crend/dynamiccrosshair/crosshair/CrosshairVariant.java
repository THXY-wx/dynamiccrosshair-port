package mod.crend.dynamiccrosshair.crosshair;

import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.config.StyleSetting;

/** A visual crosshair role. Each role has its own configurable style. */
public enum CrosshairVariant {
    NONE,
    REGULAR,
    ON_BLOCK,
    ON_ENTITY,
    HOLDING_BLOCK,
    HOLDING_TOOL,
    HOLDING_MELEE,
    HOLDING_RANGED,
    HOLDING_THROWABLE,
    HOLDING_USABLE,
    HOLDING_SHIELD,
    CAN_INTERACT;

    public StyleSetting style() {
        switch (this) {
            case REGULAR: return Config.regular;
            case ON_BLOCK: return Config.onBlock;
            case ON_ENTITY: return Config.onEntity;
            case HOLDING_BLOCK: return Config.holdingBlockStyle;
            case HOLDING_TOOL: return Config.holdingToolStyle;
            case HOLDING_MELEE: return Config.holdingMeleeStyle;
            case HOLDING_RANGED: return Config.holdingRangedStyle;
            case HOLDING_THROWABLE: return Config.holdingThrowableStyle;
            case HOLDING_USABLE: return Config.holdingUsableStyle;
            case HOLDING_SHIELD: return Config.shieldStyle;
            case CAN_INTERACT: return Config.interactStyle;
            default: return Config.regular;
        }
    }
}
