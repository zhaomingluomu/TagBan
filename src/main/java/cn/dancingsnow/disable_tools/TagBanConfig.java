package cn.dancingsnow.disable_tools;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Arrays;
import java.util.List;

public class TagBanConfig {
    // 配置规范和实例的单例模式
    // Configuration specification and singleton instance
    public static final ForgeConfigSpec SPEC;
    public static final TagBanConfig INSTANCE;

    static {
        // 构建配置规范并创建实例
        // Build configuration specification and create instance
        final Pair<TagBanConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(TagBanConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    // 配置值：工具黑名单、白名单和提示文本
    // Configuration values: tool blacklist, whitelist and tooltip text
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> tools;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> whitelist;
    public final ForgeConfigSpec.ConfigValue<String> tooltip;

    public TagBanConfig(ForgeConfigSpec.Builder builder) {
        // 开始构建配置的通用部分
        // Start building the general section of configuration
        builder.comment("禁用工具配置").push("general");

        tools = builder
                .comment(
                        // 黑名单配置的详细说明
                        // Detailed description of blacklist configuration
                        "黑名单：使用正则表达式禁用工具",
                        "Blacklist: Use regular expressions to disable tools",
                        "===禁用对盔甲无效===",
                        "===Does not affect armor===",
                        // 各种示例说明
                        // Various examples
                        "=== 示例 ===",
                        "=== Examples ===",
                        "=== 禁用全部同类型工具 ===",
                        "=== Disable all tools of the same type ===",
                        "  - '.*:.*_axe'         禁用所有斧头（所有模组）",
                        "  - '.*:.*_axe'         Disable all axes (all mods)",
                        "  - '.*:.*_pickaxe'     禁用所有镐",
                        "  - '.*:.*_pickaxe'     Disable all pickaxes",
                        // ... 更多示例
                        // ... more examples
                        "禁用对盔甲 复活图腾等无效 仅针对玩家手持物品(攻击，放置及主动使用的生效)",
                        "Does not affect armor, totems of undying, only affects player held items (attacking, placing and active use)",
                        "注意：支持所有原版和模组工具，只需替换对应的命名空间和路径",
                        "Note: Supports all vanilla and modded tools, just replace the corresponding namespace and path",
                        "默认: 禁用所有原版工具",
                        "Default: Disable all vanilla tools"
                )
                // 定义黑名单列表，默认禁用所有原版工具
                // Define blacklist, default disable all vanilla tools
                .defineList("tools",
                        Arrays.asList(
                                "minecraft:.*_axe",
                                "minecraft:.*_hoe",
                                "minecraft:.*_pickaxe",
                                "minecraft:.*_shovel",
                                "minecraft:.*_sword"
                        ),
                        obj -> obj instanceof String);

        whitelist = builder
                .comment(
                        // 白名单配置的详细说明
                        // Detailed description of whitelist configuration
                        "白名单：使用正则表达式允许工具（优先级高于黑名单）",
                        "Whitelist: Use regular expressions to allow tools (higher priority than blacklist)",
                        "写法跟黑名单一模一样",
                        "Same format as blacklist",
                        "=== 示例 ===",
                        "=== Examples ===",
                        "=== 启用指定工具 ===",
                        "=== Enable specific tools ===",
                        "  - 'minecraft:iron_sword'    启用铁剑",
                        "  - 'minecraft:iron_sword'    Enable iron sword"
                )
                // 定义白名单列表，默认为空
                // Define whitelist, default empty
                .defineList("whitelist",
                        Arrays.asList(),
                        obj -> obj instanceof String);

        tooltip = builder
                .comment("显示在禁用工具提示框中的文本", "支持翻译文本",
                        "Text displayed in disabled tool tooltip", "Supports translation text")
                .define("tooltip", "tooltip.tagban");

        builder.pop();
    }
}