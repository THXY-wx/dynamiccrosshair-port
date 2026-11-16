package mod.crend.dynamiccrosshair.compat;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Optional Tinkers' Construct support, reflection only (no compile-time dependency).
 *
 * <p>TiC's bows and crossbows implement `tconstruct.library.weaponry.IWindup`; the crossbow does
 * not use vanilla's {@code isUsingItem()} while winding, so the progress has to come from that
 * interface. Same accessors as Crosshair-Selection's TicCrosshairCompat.</p>
 */
public final class TinkersCompat {

    private static final Class<?> IWINDUP = classOrNull("tconstruct.library.weaponry.IWindup");
    private static final Class<?> CROSSBOW = classOrNull("tconstruct.weaponry.weapons.Crossbow");

    private static final Method[] WINDUP_PROGRESS = {
            methodOrNull(IWINDUP, "getWindupProgress", ItemStack.class, EntityPlayer.class),
            methodOrNull(IWINDUP, "getWindupProgress", EntityPlayer.class, ItemStack.class),
            methodOrNull(IWINDUP, "getWindupProgress", ItemStack.class),
            methodOrNull(IWINDUP, "getWindupProgress", EntityPlayer.class),
            methodOrNull(IWINDUP, "getWindupProgress")
    };
    private static final Method MIN_WINDUP = methodOrNull(IWINDUP, "getMinWindupProgress", ItemStack.class);

    private TinkersCompat() {}

    public static boolean isAvailable() {
        return IWINDUP != null;
    }

    public static boolean isWindup(ItemStack stack) {
        if (stack == null || stack.getItem() == null || IWINDUP == null) return false;
        if (!IWINDUP.isInstance(stack.getItem())) return false;
        return stack.getTagCompound() != null || (CROSSBOW != null && CROSSBOW.isInstance(stack.getItem()));
    }

    public static boolean isCrossbow(ItemStack stack) {
        return stack != null && stack.getItem() != null && CROSSBOW != null && CROSSBOW.isInstance(stack.getItem());
    }

    /** 0..1 wind-up progress; 0 when not winding. */
    public static float progress(ItemStack stack, EntityPlayer player) {
        if (!isWindup(stack) || player == null) return 0.0F;
        float raw = rawWindup(stack, player);
        float min = minWindup(stack);
        float normalised = raw / (min > 0.0F ? min : 1.0F);
        if (normalised < 0.0F) return 0.0F;
        return normalised > 1.0F ? 1.0F : normalised;
    }

    public static boolean isChargedCrossbow(ItemStack stack, EntityPlayer player) {
        return isCrossbow(stack) && progress(stack, player) >= 0.995F;
    }

    private static float rawWindup(ItemStack stack, EntityPlayer player) {
        for (Method method : WINDUP_PROGRESS) {
            if (method == null) continue;
            Object[] args = argsFor(method, stack, player);
            if (args == null) continue;
            try {
                Object value = method.invoke(stack.getItem(), args);
                if (value instanceof Number) return ((Number) value).floatValue();
            } catch (Throwable ignored) {
                // try the next signature
            }
        }
        return player.getItemInUseDuration() / 20.0F;
    }

    private static float minWindup(ItemStack stack) {
        if (MIN_WINDUP == null) return 0.0F;
        try {
            Object value = MIN_WINDUP.invoke(stack.getItem(), stack);
            if (value instanceof Number) return ((Number) value).floatValue();
        } catch (Throwable ignored) {
            // treat as 1.0
        }
        return 0.0F;
    }

    private static Object[] argsFor(Method method, ItemStack stack, EntityPlayer player) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == ItemStack.class) {
                args[i] = stack;
            } else if (types[i].isAssignableFrom(EntityPlayer.class)) {
                args[i] = player;
            } else {
                return null;
            }
        }
        return args;
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) return null;
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
