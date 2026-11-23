package mod.crend.dynamiccrosshair.compat;

import mod.crend.dynamiccrosshair.api.InteractionType;
import mod.crend.dynamiccrosshair.config.Config;
import mod.crend.dynamiccrosshair.config.Policy;
import mod.crend.dynamiccrosshair.crosshair.CrosshairContext;
import mod.crend.dynamiccrosshair.util.Reflect;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.world.World;

/**
 * Vanilla 1.7.10 heuristics. This replaces the per-mod mixins of DynamicCrosshairCompat with a
 * generic classifier plus explicit id overrides (see the compat section of the config), which is
 * what makes hundreds of GTNH blocks and items work without dedicated code.
 */
public final class Compat {

    private Compat() {}

    public static boolean isInteractable(CrosshairContext context) {
        if (context.block == null) return false;
        String id = context.blockId();
        if (Config.isBlockForcedNonInteractable(id, context.meta)) return false;
        if (Config.isBlockForcedInteractable(id, context.meta)) return true;
        if (context.block.hasTileEntity(context.meta)) return true;
        return Reflect.overrides(context.block.getClass(), Block.class,
                new String[]{"onBlockActivated", "func_149727_a"},
                World.class, int.class, int.class, int.class, EntityPlayer.class, int.class,
                float.class, float.class, float.class);
    }

    public static InteractionType classifyEntity(Entity entity) {
        if (entity == null) return InteractionType.EMPTY;
        if (entity instanceof EntityItem) return InteractionType.PICK_UP_ENTITY;
        if (entity instanceof EntityMinecart || entity instanceof EntityBoat) return InteractionType.MOUNT_ENTITY;
        if (entity instanceof EntityVillager || entity instanceof EntityTameable
                || entity instanceof EntityAnimal || entity instanceof EntityHorse) {
            return InteractionType.INTERACT_WITH_ENTITY;
        }
        return InteractionType.TARGET_ENTITY;
    }

    public static InteractionType classifyItem(CrosshairContext context) {
        ItemStack stack = context.held;
        if (stack == null || stack.getItem() == null) return InteractionType.EMPTY;
        Item item = stack.getItem();
        String id = context.itemId();
        int meta = context.itemMeta();

        if (Config.isItemForcedNonUsable(id, meta)) return InteractionType.EMPTY;

        if (item instanceof ItemBlock) return InteractionType.PLACE_BLOCK;
        if (item instanceof ItemSword) return InteractionType.MELEE_WEAPON;
        if (Config.useTinkersWeapons && TinkersCompat.isMeleeWeapon(stack)) return InteractionType.MELEE_WEAPON;
        if (Config.useModdedTools && isGenericMelee(stack)) return InteractionType.MELEE_WEAPON;
        if (item instanceof ItemBow) {
            boolean drawing = context.player != null && context.player.isUsingItem()
                    && context.player.getItemInUse() == stack;
            return drawing ? InteractionType.RANGED_WEAPON_CHARGING : InteractionType.RANGED_WEAPON;
        }
        if (TinkersCompat.isWindup(stack)) return InteractionType.RANGED_WEAPON;
        if (item instanceof ItemFood) return InteractionType.CONSUME_ITEM;
        if (item instanceof ItemPotion) return InteractionType.USE_ITEM;
        if (item instanceof ItemEnderPearl || item instanceof ItemSnowball || item instanceof ItemEgg
                || item instanceof ItemExpBottle) {
            return InteractionType.THROW_ITEM;
        }
        if (item instanceof ItemFishingRod) return InteractionType.USE_ITEM;
        if (Config.useTinkersWeapons && TinkersCompat.isHarvestTool(stack)) return classifyTool(context, stack);
        if (Config.useModdedTools && isGenericTool(stack)) return classifyTool(context, stack);
        if (isTool(item, stack)) return classifyTool(context, stack);
        if (item instanceof ItemHoe || item instanceof ItemShears || item instanceof ItemFlintAndSteel
                || item instanceof ItemBucket) {
            return InteractionType.USE_ITEM_ON_BLOCK;
        }
        if (item instanceof ItemArmor) return InteractionType.EQUIP_ITEM;
        if (item instanceof ItemRecord) return InteractionType.USE_ITEM;
        if (Config.isItemForcedUsable(id, meta)) return InteractionType.USE_ITEM;
        if (isCustomUsable(item)) return InteractionType.USE_ITEM;
        return InteractionType.EMPTY;
    }

    private static boolean wailaChecked;
    private static java.lang.reflect.Method wailaCanHarvest;

    /** Exact WailaHarvestability result, or null when the addon is absent. */
    public static Boolean wailaCanHarvest(ItemStack stack, Block block, int meta) {
        if (!wailaChecked) {
            wailaChecked = true;
            try {
                Class<?> helper = Class.forName("squeek.wailaharvestability.helpers.ToolHelper");
                wailaCanHarvest = helper.getMethod("canToolHarvestBlock",
                        ItemStack.class, Block.class, int.class);
            } catch (Throwable ignored) {
                wailaCanHarvest = null;
            }
        }
        if (wailaCanHarvest == null) return null;
        try {
            return (Boolean) wailaCanHarvest.invoke(null, stack, block, meta);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final String[] HARVEST_CLASSES = {"pickaxe", "axe", "shovel", "hoe", "sword"};

    /** Forge tool API used by GregTech, Railcraft, BuildCraft and most modded tools. */
    public static boolean isGenericMelee(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        java.util.Set<String> classes = stack.getItem().getToolClasses(stack);
        return classes != null && classes.contains("sword");
    }

    public static boolean isGenericTool(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        java.util.Set<String> classes = stack.getItem().getToolClasses(stack);
        if (classes != null) {
            for (String toolClass : HARVEST_CLASSES) {
                if (classes.contains(toolClass)) return true;
            }
        }
        for (String toolClass : HARVEST_CLASSES) {
            if (stack.getItem().getHarvestLevel(stack, toolClass) >= 0) return true;
        }
        return false;
    }

    public static boolean isHeldTool(ItemStack stack) {
        return stack != null && stack.getItem() != null && isTool(stack.getItem(), stack);
    }

    private static boolean isTool(Item item, ItemStack stack) {
        if (item instanceof ItemTool) return true;
        if (item.isItemTool(stack) && !(item instanceof ItemSword)) return true;
        // GTNH tools (GregTech, Tinkers' Construct, ...) are plain Items that override a dig-speed method.
        return Reflect.overridesDigSpeed(item.getClass());
    }

    private static InteractionType classifyTool(CrosshairContext context, ItemStack stack) {
        if (!context.withBlock || context.block == null) return InteractionType.TOOL;
        Block block = context.block;
        if (block.getMaterial().isToolNotRequired()) return InteractionType.TOOL;
        float speed = stack.getItem().getDigSpeed(stack, block, context.meta);
        boolean effective = speed > 1.0F;
        String toolClass = block.getHarvestTool(context.meta);
        int level = toolClass == null ? -1 : stack.getItem().getHarvestLevel(stack, toolClass);
        // Some modded tools do not report a harvest level (-1); trust the dig speed in that case.
        boolean canHarvest = level < 0 || block.getMaterial().isToolNotRequired()
                || block.getHarvestLevel(context.meta) <= level;
        return effective && canHarvest ? InteractionType.CORRECT_TOOL : InteractionType.INCORRECT_TOOL;
    }

    private static boolean isCustomUsable(Item item) {
        Class<?> type = item.getClass();
        return Reflect.overrides(type, Item.class, new String[]{"onItemRightClick", "func_77659_a"},
                ItemStack.class, World.class, EntityPlayer.class)
                || Reflect.overrides(type, Item.class, new String[]{"onItemUse", "func_77648_a"},
                        ItemStack.class, EntityPlayer.class, World.class, int.class, int.class, int.class,
                        int.class, float.class, float.class, float.class);
    }

    public static boolean isPolicyAllowed(Policy policy, boolean condition) {
        switch (policy) {
            case ALWAYS: return true;
            case IF_INTERACTABLE: return condition;
            default: return false;
        }
    }

    /** Whether the targeted position can be replaced by a block placement. */
    public static boolean canPlaceAt(CrosshairContext context) {
        if (!context.withBlock || context.block == null) return true;
        return context.block.isReplaceable(context.world, context.x, context.y, context.z)
                || context.block.getMaterial() == net.minecraft.block.material.Material.air;
    }
}
