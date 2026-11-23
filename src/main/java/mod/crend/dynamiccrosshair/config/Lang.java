package mod.crend.dynamiccrosshair.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;

/** Tiny runtime localisation for the config screen (zh_CN when the game language is Chinese). */
public final class Lang {

    private static final Map<String, String> ZH = new HashMap<>();

    static {
        put("Dynamic Crosshair", "动态准星");
        put("General", "通用");
        put("Dynamic", "动态");
        put("Styles", "样式");
        put("Bobbing", "摇晃");
        put("Compat", "兼容");
        put("Mod enabled", "模组开关");
        put("Dynamic crosshair", "动态准星");
        put("Hide with screen", "打开界面时隐藏");
        put("Hide with map", "手持地图时隐藏");
        put("Third person", "第三人称");
        put("Hide on F3", "F3 调试时隐藏");
        put("Correct tool dots", "正确工具圆点");
        put("Bob strength", "摇晃强度");
        put("Reset defaults", "恢复默认");
        put("Done", "完成");
        put("Save & close", "保存并关闭");
        put("Save", "保存");
        put("Reset", "重置");
        put("On block", "指向方块");
        put("Interactable block", "可交互方块");
        put("On entity", "指向实体");
        put("Holding tool", "手持工具");
        put("Holding melee", "手持近战");
        put("Holding ranged", "手持远程");
        put("Holding throwable", "手持投掷物");
        put("Holding block", "手持方块");
        put("Holding usable", "手持可用物品");
        put("View bobbing", "视角晃动");
        put("Sneak", "潜行");
        put("Item switch", "切换物品");
        put("Place shake", "放置抖动");
        put("Attack", "攻击");
        put("Eat", "进食");
        put("Bow", "拉弓");
        put("Bow indicator", "蓄力提示");
        put("Adopt colour/flow", "沿用颜色/流彩");
        put("Adopt outline", "沿用描边");
        put("Vector renderer", "平滑矢量渲染");
        put("Waila tool dot", "Waila 工具点");
        put("TiC weapons", "匠魂武器");
        put("Weak tool dot", "工具不匹配点");
        put("Modded tools", "模组工具");
        put("Block overrides", "方块覆盖");
        put("Item overrides", "物品覆盖");
        put("Save config", "保存配置");
        put("Regular", "普通");
        put("Interact", "可交互");
        put("Correct tool", "正确工具");
        put("Wrong tool", "错误工具");
        put("ON", "开");
        put("OFF", "关");
        put("Crosshair-Selection detected", "已检测到 Crosshair-Selection");
        put("Crosshair-Selection not installed", "未安装 Crosshair-Selection");
        put("AUTO yields when it draws, FORCE_OURS draws ours", "AUTO：它绘制时让出；FORCE_OURS：绘制本模组准星");
        put("ALWAYS", "总是");
        put("IF_INTERACTABLE", "可交互时");
        put("DISABLED", "禁用");
    }

    private Lang() {}

    private static void put(String english, String chinese) {
        ZH.put(english, chinese);
    }

    public static String t(String english) {
        if (english == null || english.isEmpty()) return english;
        try {
            String code = Minecraft.getMinecraft().getLanguageManager()
                    .getCurrentLanguage().getLanguageCode();
            if (code != null && code.toLowerCase(Locale.ROOT).startsWith("zh")) {
                String translated = ZH.get(english);
                if (translated != null) return translated;
            }
        } catch (Throwable ignored) {
            // fall back to English
        }
        return english;
    }
}
