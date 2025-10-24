package cn.dancingsnow.disable_tools;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)  // 仅在客户端运行 / Only run on client
public class TagBanConfigScreen {

    public static Screen create(Screen parent) {
        // 创建配置构建器
        // Create config builder
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.tagban.config"))
                .setSavingRunnable(TagBanConfigScreen::saveConfig);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.tagban.general"));

        // 获取当前配置值
        // Get current configuration values
        List<String> currentTools = new ArrayList<>();
        for (String item : TagBanConfig.INSTANCE.tools.get()) {
            currentTools.add(item);
        }

        List<String> currentWhitelist = new ArrayList<>();
        for (String item : TagBanConfig.INSTANCE.whitelist.get()) {
            currentWhitelist.add(item);
        }

        // 黑名单配置项
        // Blacklist configuration entry
        general.addEntry(entryBuilder.startStrList(Component.translatable("config.tagban.tools"),
                        currentTools)
                .setDefaultValue(Arrays.asList(
                        "minecraft:.*_axe",
                        "minecraft:.*_hoe",
                        "minecraft:.*_pickaxe",
                        "minecraft:.*_shovel",
                        "minecraft:.*_sword"
                ))
                .setTooltip(Component.translatable("tooltip.tagban.tools"))
                .setSaveConsumer(newValue -> {
                    // 保存配置并重新加载
                    // Save configuration and reload
                    TagBanConfig.INSTANCE.tools.set(newValue);
                    TagBanMod.reloadConfig();
                })
                .build());

        // 白名单配置项
        // Whitelist configuration entry
        general.addEntry(entryBuilder.startStrList(Component.translatable("config.tagban.whitelist"),
                        currentWhitelist)
                .setDefaultValue(Arrays.asList())
                .setTooltip(Component.translatable("tooltip.tagban.whitelist"))
                .setSaveConsumer(newValue -> {
                    TagBanConfig.INSTANCE.whitelist.set(newValue);
                    TagBanMod.reloadConfig();
                })
                .build());

        // 提示文本配置项
        // Tooltip text configuration entry
        general.addEntry(entryBuilder.startTextField(Component.translatable("config.tagban.tooltip"),
                        TagBanConfig.INSTANCE.tooltip.get())
                .setDefaultValue("tooltip.tagban")
                .setTooltip(Component.translatable("tooltip.tagban.tooltip"))
                .setSaveConsumer(newValue -> TagBanConfig.INSTANCE.tooltip.set(newValue))
                .build());

        return builder.build();
    }

    private static void saveConfig() {
        // 保存配置到文件
        // Save configuration to file
        TagBanConfig.SPEC.save();
        TagBanMod.reloadConfig();
    }
}