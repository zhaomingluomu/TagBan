package cn.dancingsnow.disable_tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TagBanMod.MODID)  // 注册事件订阅器 / Register event subscriber
public class TagBanEventListener {

    @SubscribeEvent
    public static void onPlayerLeftBlock(PlayerInteractEvent event) {
        // 玩家交互事件处理 - 阻止使用禁用工具
        // Player interaction event handling - prevent using disabled tools
        if (event.isCancelable() && !event.getItemStack().isEmpty()) {
            event.setCanceled(TagBanMod.isItemDisabled(event.getItemStack().getItem()));
        }
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        // 攻击实体事件处理 - 阻止使用禁用工具攻击
        // Attack entity event handling - prevent attacking with disabled tools
        if (event.isCancelable()) {
            ItemStack mainHand = event.getEntity().getMainHandItem();
            if (!mainHand.isEmpty()) {
                event.setCanceled(TagBanMod.isItemDisabled(mainHand.getItem()));
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        // 物品提示事件处理 - 为禁用工具添加提示
        // Item tooltip event handling - add tooltip for disabled tools
        ItemStack stack = event.getItemStack();
        if (!stack.isEmpty() && TagBanMod.isItemDisabled(stack.getItem())) {
            event.getToolTip().add(1, Component.translatable(TagBanMod.getTooltipText()).withStyle(ChatFormatting.RED));
        }
    }
}