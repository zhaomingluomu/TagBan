// TagBanConfig.java
package cn.dancingsnow.disable_tools;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuration class for TagBan mod.
 * 配置文件类，管理 TagBan 模组的各种黑白名单及提示文本。
 */
public class TagBanConfig {
    public static final ForgeConfigSpec SPEC;
    public static final TagBanConfig INSTANCE;

    public static final List<String> DEFAULT_GLOBAL_BLACK = Arrays.asList(
            "minecraft:.*_axe", "minecraft:.*_hoe", "minecraft:.*_pickaxe",
            "minecraft:.*_shovel", "minecraft:.*_sword"
    );
    public static final List<String> DEFAULT_EMPTY_LIST = Collections.emptyList();
    public static final boolean DEFAULT_ENABLE_SCREEN = true;

    public static final String DEF_G_TT = "tooltip.tagban.global";
    public static final String DEF_BREAK_TT = "tooltip.tagban.break";
    public static final String DEF_ATTACK_TT = "tooltip.tagban.attack";
    public static final String DEF_INTERACT_TT = "tooltip.tagban.interact";
    public static final String DEF_USE_TT = "tooltip.tagban.use";
    public static final String DEF_ARMOR_TT = "tooltip.tagban.armor";

    static {
        final Pair<TagBanConfig, ForgeConfigSpec> specPair =
                new ForgeConfigSpec.Builder().configure(TagBanConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    public final ForgeConfigSpec.ConfigValue<List<? extends String>> globalBlacklist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> globalWhitelist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> breakBlacklist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> attackBlacklist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> interactBlacklist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> useBlacklist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> armorBlacklist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> breakWhitelist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> attackWhitelist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> interactWhitelist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> useWhitelist;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> armorWhitelist;

    public final ForgeConfigSpec.ConfigValue<String> globalTooltip;
    public final ForgeConfigSpec.ConfigValue<String> breakTooltip;
    public final ForgeConfigSpec.ConfigValue<String> attackTooltip;
    public final ForgeConfigSpec.ConfigValue<String> interactTooltip;
    public final ForgeConfigSpec.ConfigValue<String> useTooltip;
    public final ForgeConfigSpec.ConfigValue<String> armorTooltip;
    public final ForgeConfigSpec.BooleanValue enableConfigScreen;

    public TagBanConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("TagBan Comprehensive Interception Config / TagBan 综合拦截配置")
                .push("general");

        globalBlacklist = builder
                .comment("Global blacklist patterns (regex). Items matching any pattern are banned from ALL actions.")
                .defineList("globalBlacklist", DEFAULT_GLOBAL_BLACK, TagBanConfig::validateString);
        globalWhitelist = builder
                .comment("Global whitelist patterns (regex). Items matching are EXEMPT from global blacklist.")
                .defineList("globalWhitelist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);

        builder.comment("Per-action blacklists").push("blacklists");
        breakBlacklist = builder.defineList("breakBlacklist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        attackBlacklist = builder.defineList("attackBlacklist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        interactBlacklist = builder.defineList("interactBlacklist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        useBlacklist = builder.defineList("useBlacklist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        armorBlacklist = builder.defineList("armorBlacklist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        builder.pop();

        builder.comment("Per-action whitelists (override blacklists)").push("whitelists");
        breakWhitelist = builder.defineList("breakWhitelist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        attackWhitelist = builder.defineList("attackWhitelist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        interactWhitelist = builder.defineList("interactWhitelist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        useWhitelist = builder.defineList("useWhitelist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        armorWhitelist = builder.defineList("armorWhitelist", DEFAULT_EMPTY_LIST, TagBanConfig::validateString);
        builder.pop();

        builder.comment("Tooltip translation keys").push("tooltips");
        globalTooltip = builder.define("globalTooltip", DEF_G_TT);
        breakTooltip = builder.define("breakTooltip", DEF_BREAK_TT);
        attackTooltip = builder.define("attackTooltip", DEF_ATTACK_TT);
        interactTooltip = builder.define("interactTooltip", DEF_INTERACT_TT);
        useTooltip = builder.define("useTooltip", DEF_USE_TT);
        armorTooltip = builder.define("armorTooltip", DEF_ARMOR_TT);
        builder.pop();

        enableConfigScreen = builder
                .comment("Enable Cloth Config screen (requires Cloth Config mod installed)")
                .define("enableConfigScreen", DEFAULT_ENABLE_SCREEN);
        builder.pop();
    }

    /**
     * Validator for config list entries - must be non-null String.
     * 配置列表条目验证器 - 必须是非空字符串。
     */
    private static boolean validateString(Object obj) {
        return obj instanceof String;
    }
}