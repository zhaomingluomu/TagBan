// TagBanMod.java
package cn.dancingsnow.disable_tools;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Main mod class for TagBan.
 * TagBan 模组的主类。
 */
@Mod(TagBanMod.MODID)
public class TagBanMod {
    public static final String MODID = "tagban";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** All action types supported by TagBan */
    public static final String[] ACTION_TYPES = {"BREAK", "ATTACK", "INTERACT", "USE", "ARMOR"};

    // Thread-safe maps for patterns and caches
    // 线程安全的模式和缓存映射
    private static final Map<String, List<Pattern>> patternsMap = new ConcurrentHashMap<>();
    private static final Map<String, Map<Item, Boolean>> cachesMap = new ConcurrentHashMap<>();

    // Track whether Cloth Config is available at runtime
    // 追踪 Cloth Config 是否在运行时可用
    private static boolean clothConfigAvailable = false;

    public TagBanMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config file
        // 注册配置文件
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TagBanConfig.SPEC, "tagban.toml");
        modEventBus.addListener(this::onCommonSetup);

        // Detect Cloth Config availability
        // 检测 Cloth Config 是否可用
        clothConfigAvailable = isModLoaded("cloth-config") || isModLoaded("cloth_config");
        LOGGER.info("[TagBan] Cloth Config detected: {}", clothConfigAvailable);

        // Only register config screen on client side when Cloth Config is present
        // 仅在客户端且 Cloth Config 存在时注册配置界面
        if (FMLEnvironment.dist.isClient() && clothConfigAvailable) {
            modEventBus.addListener(this::onClientSetup);
        }
    }

    /**
     * Common setup: compiles patterns from config.
     * 通用初始化：从配置编译正则模式。
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            compilePatterns();
            LOGGER.info("[TagBan] Patterns compiled successfully. {} global blacklist entries.",
                    patternsMap.getOrDefault("GLOBAL_BLACK", Collections.emptyList()).size());
        });
    }

    /**
     * Client setup: registers the config screen if Cloth Config is available.
     * 客户端初始化：如果 Cloth Config 可用，注册配置界面。
     *
     * This method uses reflection-free approach: the actual Cloth Config imports
     * are isolated in TagBanConfigScreen class, which is only loaded when this
     * method is called (and we've already verified Cloth Config is present).
     *
     * 此方法使用无反射方式：实际的 Cloth Config 导入隔离在 TagBanConfigScreen 类中，
     * 该类仅在此方法被调用时加载（我们已经验证 Cloth Config 存在）。
     */
    private void onClientSetup(final net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                TagBanConfigScreenRegistrar.register();
                LOGGER.info("[TagBan] Config screen registered successfully.");
            } catch (Throwable e) {
                LOGGER.warn("[TagBan] Failed to register config screen: {}", e.getMessage());
                clothConfigAvailable = false;
            }
        });
    }

    /**
     * Compiles all pattern lists from config and clears caches.
     * 从配置编译所有模式列表，并清空缓存。
     */
    public static void compilePatterns() {
        patternsMap.clear();
        cachesMap.clear();

        patternsMap.put("GLOBAL_WHITE", compileList(TagBanConfig.INSTANCE.globalWhitelist.get()));
        patternsMap.put("GLOBAL_BLACK", compileList(TagBanConfig.INSTANCE.globalBlacklist.get()));

        for (String t : ACTION_TYPES) {
            patternsMap.put(t + "_BLACK", compileList(getConfigList(t, false)));
            patternsMap.put(t + "_WHITE", compileList(getConfigList(t, true)));
            cachesMap.put(t, new ConcurrentHashMap<>());
        }
    }

    /**
     * Helper to get the raw config list for a given type and whitelist/blacklist.
     * 辅助方法：获取给定类型和白/黑名单的原始配置列表。
     */
    @SuppressWarnings("unchecked")
    private static List<? extends String> getConfigList(String type, boolean isWhite) {
        TagBanConfig cfg = TagBanConfig.INSTANCE;
        if ("BREAK".equals(type)) return isWhite ? cfg.breakWhitelist.get() : cfg.breakBlacklist.get();
        if ("ATTACK".equals(type)) return isWhite ? cfg.attackWhitelist.get() : cfg.attackBlacklist.get();
        if ("INTERACT".equals(type)) return isWhite ? cfg.interactWhitelist.get() : cfg.interactBlacklist.get();
        if ("USE".equals(type)) return isWhite ? cfg.useWhitelist.get() : cfg.useBlacklist.get();
        if ("ARMOR".equals(type)) return isWhite ? cfg.armorWhitelist.get() : cfg.armorBlacklist.get();
        return Collections.emptyList();
    }

    /**
     * Compiles a list of strings into a list of regex Pattern objects.
     * Invalid patterns are logged and skipped.
     * 将字符串列表编译为正则表达式 Pattern 对象列表。
     * 无效模式会被记录日志并跳过。
     */
    private static List<Pattern> compileList(List<? extends String> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();

        List<Pattern> ps = new ArrayList<>(list.size());
        for (String s : list) {
            if (s == null || s.trim().isEmpty()) continue;
            try {
                ps.add(Pattern.compile(s.trim(), Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                LOGGER.warn("[TagBan] Invalid regex pattern '{}': {}", s, e.getMessage());
            }
        }
        return Collections.unmodifiableList(ps);
    }

    /**
     * Checks if an item is banned for a given action type.
     * Uses caching for performance.
     * 检查物品是否被禁止用于给定的动作类型。使用缓存提升性能。
     *
     * Priority: type whitelist > global whitelist > global blacklist > type blacklist
     * 优先级：类型白名单 > 全局白名单 > 全局黑名单 > 类型黑名单
     *
     * @param item The item to check
     * @param type The action type (BREAK, ATTACK, INTERACT, USE, ARMOR)
     * @return true if banned, false otherwise
     */
    public static boolean check(Item item, String type) {
        if (item == null || type == null) return false;

        Map<Item, Boolean> cache = cachesMap.get(type);
        if (cache == null) return false;

        return cache.computeIfAbsent(item, i -> {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(i);
            if (key == null) return false;
            String id = key.toString();

            // Type-specific whitelist has highest priority
            // 类型特定白名单优先级最高
            if (matchAny(id, type + "_WHITE")) return false;
            // Global whitelist next
            // 然后是全局白名单
            if (matchAny(id, "GLOBAL_WHITE")) return false;
            // Global blacklist
            // 全局黑名单
            if (matchAny(id, "GLOBAL_BLACK")) return true;
            // Type-specific blacklist
            // 类型特定黑名单
            return matchAny(id, type + "_BLACK");
        });
    }

    /**
     * Checks if an item ID matches any pattern in the given list.
     * 检查物品 ID 是否匹配给定列表中的任一模式。
     */
    private static boolean matchAny(String id, String listKey) {
        List<Pattern> ps = patternsMap.get(listKey);
        if (ps == null || ps.isEmpty()) return false;
        for (Pattern p : ps) {
            if (p.matcher(id).matches()) return true;
        }
        return false;
    }

    /**
     * Gets the tooltip translation key for a given action type.
     * 获取给定动作类型的提示文本本地化键。
     */
    public static String getTooltipText(String type) {
        TagBanConfig cfg = TagBanConfig.INSTANCE;
        if ("BREAK".equals(type)) return cfg.breakTooltip.get();
        if ("ATTACK".equals(type)) return cfg.attackTooltip.get();
        if ("INTERACT".equals(type)) return cfg.interactTooltip.get();
        if ("USE".equals(type)) return cfg.useTooltip.get();
        if ("ARMOR".equals(type)) return cfg.armorTooltip.get();
        return cfg.globalTooltip.get();
    }

    /**
     * Safely checks if a mod is loaded.
     * 安全地检查模组是否已加载。
     */
    public static boolean isModLoaded(String modId) {
        try {
            return ModList.get() != null && ModList.get().isLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns whether Cloth Config is available at runtime.
     * 返回 Cloth Config 是否在运行时可用。
     */
    public static boolean isClothConfigAvailable() {
        return clothConfigAvailable;
    }

    /**
     * Reloads the configuration (recompiles patterns and clears caches).
     * 重新加载配置（重新编译模式并清空缓存）。
     */
    public static void reloadConfig() {
        compilePatterns();
        LOGGER.info("[TagBan] Configuration reloaded.");
    }
}