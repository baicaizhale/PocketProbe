package org.YanPl.pocketProbe;

import org.bukkit.ChatColor;
import org.bukkit.Material; // 导入 Material 类
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; // 导入 EventHandler 注解
import org.bukkit.event.Listener; // 导入 Listener 接口
import org.bukkit.event.player.PlayerInteractAtEntityEvent; // 导入 PlayerInteractAtEntityEvent
import org.bukkit.inventory.EquipmentSlot; // 导入 EquipmentSlot 枚举

/**
 * This class listens for player events, specifically right-clicking on other entities (players).
 * It implements the Listener interface, which is required for event listeners in Spigot.
 */
public class PocketProbeListener implements Listener {

    /**
     * This method is called automatically by Spigot when a PlayerInteractAtEntityEvent occurs.
     * @param event The event object containing details about the player's interaction.
     */
    @EventHandler // This annotation tells Spigot that this method is an event handler
    public void onPlayerRightClickPlayer(PlayerInteractAtEntityEvent event) {
        // 1. 确保事件是由主手触发，避免副手也触发两次。
        // 在新版本 Minecraft 中，右键事件可能由主手和副手同时触发。
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // 2. 确保被点击的实体是一个玩家。
        if (!(event.getRightClicked() instanceof Player)) {
            return; // 如果不是玩家，则不处理
        }

        // 3. 获取右键点击的玩家 (执行者) 和被点击的玩家 (目标)。
        Player clicker = event.getPlayer(); // 右键点击的玩家
        Player targetPlayer = (Player) event.getRightClicked(); // 被右键点击的玩家

        // 4. 检查点击者是否拥有使用权限。
        if (!clicker.hasPermission("pocketprobe.use")) {
            clicker.sendMessage(ChatColor.RED + "你没有权限使用此功能。");
            return;
        }

        // 5. 阻止事件，防止右键默认行为（例如：如果你手中拿着东西，可能会对目标玩家使用物品）。
        event.setCancelled(true);

        // 6. 打开目标玩家的背包给点击者。
        clicker.openInventory(targetPlayer.getInventory());
        clicker.sendMessage(ChatColor.GREEN + "你已打开 " + targetPlayer.getName() + " 的背包。");
    }
}
