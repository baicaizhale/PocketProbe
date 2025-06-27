package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

// 移除未使用的导入语句
// import java.util.ArrayList;
// import java.util.List;
import java.util.Map;
// import java.util.stream.Collectors;
// import java.util.Arrays; // 如果没有显式使用Arrays.asList等，可以移除


/**
 * This class listens for player events, specifically right-clicking on other entities (players)
 * and inventory close events for the custom probe inventory.
 * It implements the Listener interface, which is required for event listeners in Spigot.
 */
public class PocketProbeListener implements Listener {

    /**
     * This method is called automatically by Spigot when a PlayerInteractAtEntityEvent occurs.
     * @param event The event object containing details about the player's interaction.
     */
    @EventHandler
    public void onPlayerRightClickPlayer(PlayerInteractAtEntityEvent event) {
        // 1. 确保事件是由主手触发，避免副手也触发两次。
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // 2. 确保被点击的实体是一个玩家，并使用模式变量 'targetPlayer'
        if (!(event.getRightClicked() instanceof Player targetPlayer)) {
            return; // 如果不是玩家，则不处理
        }

        // 3. 获取右键点击的玩家 (执行者)。
        Player clicker = event.getPlayer(); // 右键点击的玩家

        // 4. 检查点击者是否拥有使用权限。
        if (!clicker.hasPermission("pocketprobe.use")) {
            clicker.sendMessage(ChatColor.RED + "你没有权限使用此功能。");
            return;
        }

        // 5. 阻止事件，防止右键默认行为（例如：如果你手中拿着东西，可能会对目标玩家使用物品）。
        event.setCancelled(true);

        // 创建一个 9x6 (54格) 的自定义背包，用于显示所有物品
        Inventory probeInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "查看 " + targetPlayer.getName() + " 的背包");

        PlayerInventory targetInv = targetPlayer.getInventory();

        // 填充主物品栏 (27格) 和热启动栏 (9格)，共 36 格
        ItemStack[] storageContents = targetInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> 自定义背包槽位 45-53)
                probeInventory.setItem(45 + i, storageContents[i]);
            } else { // 主物品栏 (玩家背包槽位 9-35 -> 自定义背包槽位 18-44)
                probeInventory.setItem(18 + (i - 9), storageContents[i]);
            }
        }

        // 放置盔甲栏 (自定义背包顶部 4 格)
        probeInventory.setItem(0, targetInv.getHelmet());      // 头盔
        probeInventory.setItem(1, targetInv.getChestplate());  // 胸甲
        probeInventory.setItem(2, targetInv.getLeggings());    // 护腿
        probeInventory.setItem(3, targetInv.getBoots());       // 靴子

        // 放置副手 (自定义背包右上方槽位 8)
        probeInventory.setItem(8, targetInv.getItemInOffHand()); // 副手

        // 填充空槽位，使其看起来更整洁
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " "); // 设置为空名称以隐藏物品名
            filler.setItemMeta(fillerMeta);
        }

        // 优化填充物放置逻辑，避免重复代码段
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 36, 37, 38, 39, 40, 41, 42, 43, 44};
        for (int slot : fillerSlots) {
            probeInventory.setItem(slot, filler);
        }

        // 将自定义背包和目标玩家关联起来，以便在背包关闭时进行同步
        PocketProbe.getInstance().getOpenedProbeInventories().put(probeInventory, targetPlayer);

        // 打开自定义背包给执行命令的玩家
        clicker.openInventory(probeInventory);

        clicker.sendMessage(ChatColor.GREEN + "你已打开 " + targetPlayer.getName() + " 的背包。");
    }

    /**
     * 处理自定义背包的关闭事件，将物品同步回目标玩家的真实背包。
     * @param event 背包关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 检查关闭的背包是否是我们的自定义探查背包
        Inventory closedInventory = event.getInventory();
        Map<Inventory, Player> openedInventories = PocketProbe.getInstance().getOpenedProbeInventories();

        if (openedInventories.containsKey(closedInventory)) {
            Player targetPlayer = openedInventories.get(closedInventory);
            PlayerInventory targetInv = targetPlayer.getInventory();

            // 移除 Map 中的条目，表示此背包已关闭
            openedInventories.remove(closedInventory);

            // 同步盔甲栏
            targetInv.setHelmet(closedInventory.getItem(0));
            targetInv.setChestplate(closedInventory.getItem(1));
            targetInv.setLeggings(closedInventory.getItem(2));
            targetInv.setBoots(closedInventory.getItem(3));

            // 同步副手
            targetInv.setItemInOffHand(closedInventory.getItem(8));

            // 同步主物品栏和热启动栏（共 36 格）
            ItemStack[] newStorageContents = new ItemStack[36];
            for (int i = 0; i < 9; i++) { // 热启动栏 (自定义背包槽位 45-53 -> 玩家背包槽位 0-8)
                newStorageContents[i] = closedInventory.getItem(45 + i);
            }
            for (int i = 0; i < 27; i++) { // 主物品栏 (自定义背包槽位 18-44 -> 玩家背包槽位 9-35)
                newStorageContents[9 + i] = closedInventory.getItem(18 + i);
            }
            targetInv.setStorageContents(newStorageContents);

            // 确保玩家背包已更新
            // 注意: updateInventory() 方法可能带有 @ApiStatus.Internal 标记，
            // 表示它是内部 API，未来版本可能有变动，但目前通常安全使用。
            targetPlayer.updateInventory();

            // 可以选择性地给打开背包的玩家发送消息
            event.getPlayer().sendMessage(ChatColor.GREEN + targetPlayer.getName() + " 的背包已更新。");
        }
    }

    /**
     * 防止玩家在探查背包中移动或拿起填充物。
     * @param event 背包点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Map<Inventory, Player> openedInventories = PocketProbe.getInstance().getOpenedProbeInventories();
        Inventory currentOpenInventory = event.getInventory();

        // 只有当当前打开的背包是我们自定义的探查背包时才进行处理
        if (openedInventories.containsKey(currentOpenInventory)) {
            int slot = event.getRawSlot(); // 获取点击的原始槽位

            // 检查点击的槽位是否是填充物槽位 (4-7, 9-17, 36-44)
            int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 36, 37, 38, 39, 40, 41, 42, 43, 44};
            boolean isFillerSlot = false;
            for (int fillerSlot : fillerSlots) {
                if (slot == fillerSlot) {
                    isFillerSlot = true;
                    break;
                }
            }

            if (isFillerSlot) {
                // 阻止玩家捡起填充物或将其他物品放入填充物槽位
                // 添加对 currentOpenInventory.getItem(slot) 的 null 检查，以避免 NPE
                if (currentOpenInventory.getItem(slot) != null && currentOpenInventory.getItem(slot).getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    event.setCancelled(true); // 阻止拿起填充物
                }
                // 添加对 event.getCursor() 的 null 检查，以避免 NPE
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true); // 阻止放置物品到填充物槽位
                }
            }
        }
    }
}
