// TagBanConfigScreenRegistrar.java
package cn.dancingsnow.disable_tools;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Isolated class that handles Cloth Config screen registration.
 * This class is only loaded when Cloth Config is confirmed to be present,
 * preventing ClassNotFoundException when Cloth Config is absent.
 *
 * 隔离类，处理 Cloth Config 界面注册。
 * 此类仅在确认 Cloth Config 存在时加载，
 * 防止 Cloth Config 不存在时出现 ClassNotFoundException。
 */
@OnlyIn(Dist.CLIENT)
public class TagBanConfigScreenRegistrar {

    /**
     * Registers the config screen factory with Forge.
     * Must only be called after verifying Cloth Config is loaded.
     * 向 Forge 注册配置界面工厂。必须在验证 Cloth Config 已加载后才能调用。
     */
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> TagBanConfigScreen.create(parent)
                )
        );
    }
}