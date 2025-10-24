package cn.dancingsnow.disable_tools;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import java.util.List;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Mod(TagBanMod.MODID)
public class TagBanMod {
    // 模组常量
    // Mod constants
    public static final String MODID = "tagban";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 缓存编译后的正则表达式模式
    // Cached compiled regex patterns
    private static volatile List<Pattern> cachedBlacklistPatterns = Collections.emptyList();
    private static volatile List<Pattern> cachedWhitelistPatterns = Collections.emptyList();

    // 物品缓存和配置重载时间跟踪
    // Item cache and config reload time tracking
    private static final Map<Item, Boolean> itemCache = new ConcurrentHashMap<>();
    private static volatile long lastConfigReloadTime = 0;
    private static final long CACHE_DURATION = 5000;  // 缓存持续时间(毫秒) / Cache duration in milliseconds

    // Cloth Config 可用性检查
    // Cloth Config availability check
    private static boolean clothConfigAvailable = false;

    static {
        // 检查 Cloth Config 是否可用
        // Check if Cloth Config is available
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            clothConfigAvailable = true;
            LOGGER.info("Cloth Config 已检测到，配置界面可用 / Cloth Config detected, config screen available");
        } catch (ClassNotFoundException e) {
            clothConfigAvailable = false;
            LOGGER.info("Cloth Config 未找到，配置界面将不可用 / Cloth Config not found, config screen unavailable");
        }
    }

    public TagBanMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        // Register configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TagBanConfig.SPEC, "tagban.toml");

        modEventBus.addListener(this::onCommonSetup);

        if (clothConfigAvailable) {
            modEventBus.addListener(this::onClientSetup);
        }

        LOGGER.info("TagBan Mod 已加载 / TagBan Mod loaded");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // 通用设置 - 预编译正则表达式
        // Common setup - precompile regex patterns
        event.enqueueWork(() -> {
            compilePatterns();
            LOGGER.debug("已预编译 {} 个黑名单模式和 {} 个白名单模式 / Precompiled {} blacklist patterns and {} whitelist patterns",
                    cachedBlacklistPatterns.size(), cachedWhitelistPatterns.size(),
                    cachedBlacklistPatterns.size(), cachedWhitelistPatterns.size());
        });
    }

    @net.minecraftforge.api.distmarker.OnlyIn(Dist.CLIENT)
    private void onClientSetup(final net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        // 客户端设置 - 注册配置界面
        // Client setup - register config screen
        if (clothConfigAvailable) {
            event.enqueueWork(() -> {
                try {
                    ModLoadingContext.get().registerExtensionPoint(
                            net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                            () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                                    (client, parent) -> {
                                        try {
                                            return (net.minecraft.client.gui.screens.Screen) Class.forName("cn.dancingsnow.disable_tools.TagBanConfigScreen")
                                                    .getMethod("create", net.minecraft.client.gui.screens.Screen.class)
                                                    .invoke(null, parent);
                                        } catch (Exception e) {
                                            LOGGER.error("无法创建配置界面 / Failed to create config screen", e);
                                            return parent;
                                        }
                                    }
                            )
                    );
                    LOGGER.info("配置界面已注册 / Config screen registered");
                } catch (Exception e) {
                    LOGGER.error("注册配置界面时出错 / Error registering config screen", e);
                }
            });
        }
    }

    private static void compilePatterns() {
        // 从配置获取列表并编译为正则表达式模式
        // Get lists from config and compile into regex patterns
        List<? extends String> blacklist = TagBanConfig.INSTANCE.tools.get();
        List<? extends String> whitelist = TagBanConfig.INSTANCE.whitelist.get();

        List<Pattern> newBlacklistPatterns = Collections.synchronizedList(new ArrayList<>(blacklist.size()));
        List<Pattern> newWhitelistPatterns = Collections.synchronizedList(new ArrayList<>(whitelist.size()));

        // 并行编译黑名单正则表达式
        // Parallel compile blacklist regex patterns
        blacklist.parallelStream().forEach(regex -> {
            try {
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                newBlacklistPatterns.add(pattern);
            } catch (Exception e) {
                LOGGER.error("无效的黑名单正则表达式: '{}', 错误: {} / Invalid blacklist regex: '{}', error: {}",
                        regex, e.getMessage(), regex, e.getMessage());
            }
        });

        // 并行编译白名单正则表达式
        // Parallel compile whitelist regex patterns
        whitelist.parallelStream().forEach(regex -> {
            try {
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                newWhitelistPatterns.add(pattern);
            } catch (Exception e) {
                LOGGER.error("无效的白名单正则表达式: '{}', 错误: {} / Invalid whitelist regex: '{}', error: {}",
                        regex, e.getMessage(), regex, e.getMessage());
            }
        });

        // 更新缓存模式列表
        // Update cached pattern lists
        cachedBlacklistPatterns = new ArrayList<>(newBlacklistPatterns);
        cachedWhitelistPatterns = new ArrayList<>(newWhitelistPatterns);

        // 清空物品缓存并更新重载时间
        // Clear item cache and update reload time
        itemCache.clear();
        lastConfigReloadTime = System.currentTimeMillis();

        LOGGER.debug("正则表达式编译完成 - 黑名单: {} 条, 白名单: {} 条 / Regex compilation completed - blacklist: {}, whitelist: {}",
                cachedBlacklistPatterns.size(), cachedWhitelistPatterns.size(),
                cachedBlacklistPatterns.size(), cachedWhitelistPatterns.size());
    }

    private static void checkAndRecompilePatterns() {
        // 检查是否需要重新编译模式（缓存为空或超时）
        // Check if patterns need recompilation (cache empty or timeout)
        if ((cachedBlacklistPatterns.isEmpty() && cachedWhitelistPatterns.isEmpty()) ||
                System.currentTimeMillis() - lastConfigReloadTime > CACHE_DURATION) {
            compilePatterns();
        }
    }

    public static boolean isItemDisabled(Item item) {
        // 检查物品是否被禁用
        // Check if item is disabled
        if (item == null) return false;

        // 使用缓存避免重复计算
        // Use cache to avoid repeated calculations
        Boolean cached = itemCache.get(item);
        if (cached != null) {
            return cached;
        }

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            itemCache.put(item, false);
            return false;
        }

        String itemId = key.toString();

        // 确保模式已编译
        // Ensure patterns are compiled
        checkAndRecompilePatterns();

        // 首先检查白名单（优先级更高）
        // Check whitelist first (higher priority)
        for (Pattern pattern : cachedWhitelistPatterns) {
            if (pattern.matcher(itemId).matches()) {
                itemCache.put(item, false);
                return false;
            }
        }

        // 然后检查黑名单
        // Then check blacklist
        for (Pattern pattern : cachedBlacklistPatterns) {
            if (pattern.matcher(itemId).matches()) {
                itemCache.put(item, true);
                return true;
            }
        }

        itemCache.put(item, false);
        return false;
    }

    public static String getTooltipText() {
        // 获取提示文本
        // Get tooltip text
        return TagBanConfig.INSTANCE.tooltip.get();
    }

    public static void reloadConfig() {
        // 在单独线程中重新加载配置
        // Reload configuration in separate thread
        new Thread(() -> {
            compilePatterns();
            LOGGER.info("配置已重新加载 / Configuration reloaded");
        }, "TagBan-Config-Reload").start();
    }

    public static void clearCache() {
        // 清空物品缓存
        // Clear item cache
        itemCache.clear();
    }
}