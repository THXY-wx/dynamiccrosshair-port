package mod.crend.dynamiccrosshair.util;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection helpers that work in both a development environment (MCP names) and a production
 * GTNH client (srg names such as func_149727_a). The cache never stores null: ConcurrentHashMap
 * rejects null values, which used to crash on the first class that lacked the MCP name.
 */
public final class Reflect {

    private static final ConcurrentHashMap<String, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Boolean> DIG_SPEED_CACHE = new ConcurrentHashMap<>();

    private Reflect() {}

    public static Method method(Class<?> owner, String[] names, Class<?>... params) {
        if (owner == null || names == null || names.length == 0) return null;
        String key = key(owner, names, params);
        Optional<Method> cached = METHOD_CACHE.get(key);
        if (cached != null) return cached.orElse(null);

        Method found = null;
        for (String name : names) {
            try {
                found = owner.getMethod(name, params);
                break;
            } catch (NoSuchMethodException ignored) {
                // try the next name (srg/MCP)
            } catch (Throwable ignored) {
                // linkage problems are treated as "not overridden"
            }
        }
        METHOD_CACHE.put(key, Optional.ofNullable(found));
        return found;
    }

    /**
     * True when {@code owner} overrides the method declared on {@code base}. {@code names} should
     * contain the MCP name followed by the srg name.
     */
    public static boolean overrides(Class<?> owner, Class<?> base, String[] names, Class<?>... params) {
        Method method = method(owner, names, params);
        return method != null && method.getDeclaringClass() != base && method.getDeclaringClass() != Object.class;
    }

    /**
     * True when the item class overrides a dig-speed method. Vanilla uses the srg name
     * {@code func_150893_a(ItemStack, Block)} while Forge adds a three-argument
     * {@code getDigSpeed(ItemStack, Block, int)}; GTNH tools may override either one.
     */
    public static boolean overridesDigSpeed(Class<?> owner) {
        if (owner == null) return false;
        Boolean cached = DIG_SPEED_CACHE.get(owner);
        if (cached != null) return cached;

        boolean result = false;
        try {
            for (Method method : owner.getMethods()) {
                String name = method.getName();
                if (!"getDigSpeed".equals(name) && !"func_150893_a".equals(name)) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length >= 2
                        && params[0] == net.minecraft.item.ItemStack.class
                        && params[1] == net.minecraft.block.Block.class
                        && method.getDeclaringClass() != net.minecraft.item.Item.class) {
                    result = true;
                    break;
                }
            }
        } catch (Throwable ignored) {
            result = false;
        }
        DIG_SPEED_CACHE.put(owner, result);
        return result;
    }

    private static String key(Class<?> owner, String[] names, Class<?>[] params) {
        StringBuilder builder = new StringBuilder(owner.getName());
        builder.append('#');
        for (String name : names) builder.append(name).append('|');
        for (Class<?> param : params) builder.append(',').append(param.getName());
        return builder.toString();
    }

    public static Object getStatic(Class<?> owner, String field) {
        try {
            java.lang.reflect.Field f = owner.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object getField(Object instance, String field) {
        if (instance == null) return null;
        try {
            java.lang.reflect.Field f = instance.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(instance);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setField(Object instance, String field, Object value) {
        if (instance == null) return;
        try {
            java.lang.reflect.Field f = instance.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(instance, value);
        } catch (Throwable ignored) {}
    }
}
