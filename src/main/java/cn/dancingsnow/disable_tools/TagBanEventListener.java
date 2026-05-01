// TagBanEventListener.java
package cn.dancingsnow.disable_tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Listens to Forge events to enforce the item bans.
 * 监听 Forge 事件，用于执行物品禁用逻辑。
 */
@Mod.EventBusSubscriber(modid = TagBanMod.MODID)
public class TagBanEventListener {

    // Cooldown to prevent chat spam (in ticks)
    // 冷却时间，防止聊天刷屏（以刻为单位）
    private static final int MESSAGE_COOLDOWN = 20;

    /**
     * Adds warning tooltips to items if they are banned in any category.
     * 如果物品在任何分类中被禁用，则在物品提示栏中添加红色警告。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Item item = stack.getItem();

        for (String type : TagBanMod.ACTION_TYPES) {
            if (TagBanMod.check(item, type)) {
                String translationKey = TagBanMod.getTooltipText(type);
                try {
                    event.getToolTip().add(1,
                            Component.translatable(translationKey)
                                    .withStyle(ChatFormatting.RED));
                } catch (IndexOutOfBoundsException e) {
                    // Fallback: add at end if tooltip list is too short
                    // 回退：如果提示列表太短，添加到末尾
                    event.getToolTip().add(
                            Component.translatable(translationKey)
                                    .withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    /**
     * Prevents block breaking if the held item is banned for 'BREAK'.
     * 如果手持物品被禁止破坏，则取消方块破坏事件。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (TagBanMod.check(player.getMainHandItem().getItem(), "BREAK")) {
            event.setCanceled(true);
            sendBanMessage(player, "BREAK");
        }
    }

    /**
     * Prevents attacking entities if the held item is banned for 'ATTACK'.
     * 如果手持物品被禁止攻击，则取消攻击实体事件。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        if (TagBanMod.check(player.getMainHandItem().getItem(), "ATTACK")) {
            event.setCanceled(true);
            sendBanMessage(player, "ATTACK");
        }
    }

    /**
     * Prevents right-clicking blocks if the item is banned for 'INTERACT'.
     * 如果物品被禁止交互，则取消右键点击方块事件。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        if (TagBanMod.check(event.getItemStack().getItem(), "INTERACT")) {
            event.setCanceled(true);
            sendBanMessage(event.getEntity(), "INTERACT");
        }
    }

    /**
     * Prevents right-clicking entities if banned for 'INTERACT'.
     * Skips merchants/villagers to allow trading.
     * 拦截实体交互。跳过商人/村民以允许交易。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Allow interactions with merchants and villagers
        // 允许与商人和村民的交互
        if (event.getTarget() != null) {
            try {
                // Use class name check to avoid direct import issues across versions
                // 使用类名检查避免跨版本的直接导入问题
                String targetClass = event.getTarget().getClass().getName();
                if (isMerchantEntity(event.getTarget())) {
                    return;
                }
            } catch (Exception ignored) {
                // If class check fails, proceed with normal ban logic
                // 如果类检查失败，继续正常的禁用逻辑
            }
        }

        if (TagBanMod.check(event.getItemStack().getItem(), "INTERACT")) {
            event.setCanceled(true);
            sendBanMessage(event.getEntity(), "INTERACT");
        }
    }

    /**
     * Prevents right-clicking items if banned for 'USE' or 'ARMOR'.
     * 如果物品被禁止使用或作为盔甲，则取消右键点击物品事件。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Item item = event.getItemStack().getItem();
        if (TagBanMod.check(item, "USE")) {
            event.setCanceled(true);
            sendBanMessage(event.getEntity(), "USE");
        } else if (TagBanMod.check(item, "ARMOR")) {
            event.setCanceled(true);
            sendBanMessage(event.getEntity(), "ARMOR");
        }
    }

    /**
     * Armor ticking logic to force unequip banned items.
     * Runs server-side only, every 20 ticks.
     * 盔甲刻逻辑：强制卸下被禁用的盔甲物品。仅服务端运行，每20刻一次。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player == null || event.player.level().isClientSide) return;
        if (event.player.tickCount % 20 != 0) return;

        Player player = event.player;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                if (!armor.isEmpty() && TagBanMod.check(armor.getItem(), "ARMOR")) {
                    // Try to add to inventory, if full then drop on ground
                    // 尝试放入背包，如果满了则丢在地上
                    ItemStack copy = armor.copy();
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    if (!player.getInventory().add(copy)) {
                        player.drop(copy, false);
                    }
                    sendBanMessage(player, "ARMOR");
                }
            }
        }
    }

    /**
     * Checks if an entity is a merchant type (villager, wandering trader, etc.)
     * Uses instanceof checks that are safe across MC versions.
     * 检查实体是否为商人类型（村民、流浪商人等）。
     * 使用跨 MC 版本安全的 instanceof 检查。
     */
    private static boolean isMerchantEntity(net.minecraft.world.entity.Entity entity) {
        // Check common merchant interfaces/classes
        // 检查常见的商人接口/类
        if (entity instanceof net.minecraft.world.entity.npc.AbstractVillager) return true;
        if (entity instanceof net.minecraft.world.item.trading.Merchant) return true;
        return false;
    }

    /**
     * Sends a ban message to the player with rate limiting.
     * 向玩家发送禁用消息（带频率限制）。
     */
    private static void sendBanMessage(Player player, String type) {
        if (player == null) return;
        try {
            player.displayClientMessage(
                    Component.translatable(TagBanMod.getTooltipText(type))
                            .withStyle(ChatFormatting.RED),
                    true);
        } catch (Exception e) {
            // Silently ignore message send failures
            // 静默忽略消息发送失败
        }
    }
}