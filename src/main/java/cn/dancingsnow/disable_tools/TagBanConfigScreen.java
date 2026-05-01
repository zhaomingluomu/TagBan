// TagBanConfigScreen.java
package cn.dancingsnow.disable_tools;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration screen for TagBan mod using Cloth Config API.
 * This class is only loaded when Cloth Config is confirmed available.
 *
 * 使用 Cloth Config API 的配置界面类。
 * 此类仅在确认 Cloth Config 可用时加载。
 */
@OnlyIn(Dist.CLIENT)
public class TagBanConfigScreen {

    /**
     * Creates and returns the configuration screen.
     * 创建并返回配置界面。
     *
     * @param parent The parent screen to return to
     * @return The built configuration screen, or the parent if disabled
     */
    public static Screen create(Screen parent) {
        // If config screen is disabled, return parent directly
        // 如果配置界面被禁用，直接返回父界面
        if (!TagBanConfig.INSTANCE.enableConfigScreen.get()) {
            return parent;
        }

        try {
            return buildScreen(parent);
        } catch (Throwable e) {
            TagBanMod.LOGGER.warn("[TagBan] Failed to create config screen: {}", e.getMessage());
            return parent;
        }
    }

    /**
     * Actually builds the Cloth Config screen.
     * 实际构建 Cloth Config 界面。
     */
    private static Screen buildScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.tagban.config"))
                .setSavingRunnable(() -> {
                    TagBanConfig.SPEC.save();
                    TagBanMod.reloadConfig();
                });

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("category.tagban.general"));

        // Enable/disable config screen toggle
        // 配置界面开关
        general.addEntry(eb.startBooleanToggle(
                        Component.translatable("config.tagban.enable_screen"),
                        TagBanConfig.INSTANCE.enableConfigScreen.get())
                .setDefaultValue(TagBanConfig.DEFAULT_ENABLE_SCREEN)
                .setSaveConsumer(TagBanConfig.INSTANCE.enableConfigScreen::set)
                .build());

        // Global lists and tooltip
        // 全局列表和提示文本
        addListEntry(general, eb, "config.tagban.global_black",
                TagBanConfig.INSTANCE.globalBlacklist, TagBanConfig.DEFAULT_GLOBAL_BLACK);
        addListEntry(general, eb, "config.tagban.global_white",
                TagBanConfig.INSTANCE.globalWhitelist, TagBanConfig.DEFAULT_EMPTY_LIST);
        addTextEntry(general, eb, "config.tagban.tt_global",
                TagBanConfig.INSTANCE.globalTooltip, TagBanConfig.DEF_G_TT);

        // Per-type entries
        // 各类型条目
        String[] types = {"break", "attack", "interact", "use", "armor"};
        for (String t : types) {
            // Divider
            general.addEntry(eb.startTextDescription(
                    Component.translatable("config.tagban.divider." + t)).build());

            // Blacklist and whitelist
            addListEntry(general, eb, "config.tagban." + t + "_b",
                    getConfigValue(t, false), TagBanConfig.DEFAULT_EMPTY_LIST);
            addListEntry(general, eb, "config.tagban." + t + "_w",
                    getConfigValue(t, true), TagBanConfig.DEFAULT_EMPTY_LIST);

            // Tooltip
            addTextEntry(general, eb, "config.tagban.tt_" + t,
                    getTooltipValue(t), getDefaultTooltip(t));
        }

        return builder.build();
    }

    /**
     * Helper to add a string list entry to a category.
     * 辅助方法：向分类添加一个字符串列表条目。
     */
    @SuppressWarnings("unchecked")
    private static void addListEntry(ConfigCategory cat, ConfigEntryBuilder eb, String key,
                                     ForgeConfigSpec.ConfigValue<List<? extends String>> val,
                                     List<String> defaultVal) {
        cat.addEntry(eb.startStrList(
                        Component.translatable(key),
                        new ArrayList<>(val.get()))
                .setDefaultValue(defaultVal)
                .setSaveConsumer(val::set)
                .build());
    }

    /**
     * Helper to add a text field entry to a category.
     * 辅助方法：向分类添加一个文本字段条目。
     */
    private static void addTextEntry(ConfigCategory cat, ConfigEntryBuilder eb, String key,
                                     ForgeConfigSpec.ConfigValue<String> val, String defaultVal) {
        cat.addEntry(eb.startTextField(
                        Component.translatable(key),
                        val.get())
                .setDefaultValue(defaultVal)
                .setSaveConsumer(val::set)
                .build());
    }

    /**
     * Retrieves the ConfigValue for the given type and whether it's whitelist.
     * 根据类型和是否白名单获取对应的配置值。
     */
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> getConfigValue(String t, boolean w) {
        TagBanConfig cfg = TagBanConfig.INSTANCE;
        if ("break".equals(t)) return w ? cfg.breakWhitelist : cfg.breakBlacklist;
        if ("attack".equals(t)) return w ? cfg.attackWhitelist : cfg.attackBlacklist;
        if ("interact".equals(t)) return w ? cfg.interactWhitelist : cfg.interactBlacklist;
        if ("use".equals(t)) return w ? cfg.useWhitelist : cfg.useBlacklist;
        // armor
        return w ? cfg.armorWhitelist : cfg.armorBlacklist;
    }

    /**
     * Retrieves the tooltip ConfigValue for the given type.
     * 获取给定类型的提示文本配置值。
     */
    private static ForgeConfigSpec.ConfigValue<String> getTooltipValue(String t) {
        TagBanConfig cfg = TagBanConfig.INSTANCE;
        if ("break".equals(t)) return cfg.breakTooltip;
        if ("attack".equals(t)) return cfg.attackTooltip;
        if ("interact".equals(t)) return cfg.interactTooltip;
        if ("use".equals(t)) return cfg.useTooltip;
        return cfg.armorTooltip;
    }

    /**
     * Returns the default tooltip translation key for the given type.
     * 返回给定类型的默认提示文本本地化键。
     */
    private static String getDefaultTooltip(String t) {
        if ("break".equals(t)) return TagBanConfig.DEF_BREAK_TT;
        if ("attack".equals(t)) return TagBanConfig.DEF_ATTACK_TT;
        if ("interact".equals(t)) return TagBanConfig.DEF_INTERACT_TT;
        if ("use".equals(t)) return TagBanConfig.DEF_USE_TT;
        return TagBanConfig.DEF_ARMOR_TT;
    }
}