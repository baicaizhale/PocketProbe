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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;


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
        // 1. Ensure the event is triggered by the main hand, to avoid double-triggering by the off-hand.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // 2. Ensure the right-clicked entity is a player, and use a pattern variable 'targetPlayer'.
        if (!(event.getRightClicked() instanceof Player targetPlayer)) {
            return; // If it's not a player, do not process.
        }

        // 3. Get the player who performed the right-click (the viewer).
        Player clicker = event.getPlayer(); // The player who right-clicked.

        // 4. Check if the clicker has the necessary permission.
        if (!clicker.hasPermission("pocketprobe.use")) {
            clicker.sendMessage(ChatColor.RED + "你没有权限使用此功能。");
            return;
        }

        // 5. Cancel the event to prevent default right-click behavior (e.g., using an item on the target player if held).
        event.setCancelled(true);

        // Create a 9x6 (54 slots) custom inventory for displaying all items.
        Inventory probeInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "查看 " + targetPlayer.getName() + " 的背包");

        PlayerInventory targetInv = targetPlayer.getInventory();

        // Populate main inventory (27 slots) and hotbar (9 slots), total 36 slots.
        ItemStack[] storageContents = targetInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            // 0-8 are hotbar slots, 9-35 are main inventory slots.
            // In the custom GUI, we want the hotbar at the bottom (slots 45-53) and main inventory above it (slots 18-44).
            if (i <= 8) { // Hotbar (player inventory slots 0-8 -> custom GUI slots 45-53)
                probeInventory.setItem(45 + i, storageContents[i]);
            } else { // Main inventory (player inventory slots 9-35 -> custom GUI slots 18-44)
                probeInventory.setItem(18 + (i - 9), storageContents[i]);
            }
        }

        // Place armor slots (top 4 slots of the custom GUI)
        probeInventory.setItem(0, targetInv.getHelmet());      // Helmet
        probeInventory.setItem(1, targetInv.getChestplate());  // Chestplate
        probeInventory.setItem(2, targetInv.getLeggings());    // Leggings
        probeInventory.setItem(3, targetInv.getBoots());       // Boots

        // Place off-hand item (top-right slot 8 of the custom GUI)
        probeInventory.setItem(8, targetInv.getItemInOffHand()); // Off-hand

        // Fill empty slots to make it look tidy.
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " "); // Set to empty name to hide item name.
            filler.setItemMeta(fillerMeta);
        }

        // Optimized filler placement logic, avoiding redundant code.
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : fillerSlots) {
            probeInventory.setItem(slot, filler);
        }

        // 创建新的探查会话
        ProbeSession session = new ProbeSession(targetPlayer, probeInventory, clicker);

        // 将自定义背包和会话关联起来，以便在背包关闭时进行同步
        PocketProbe.getInstance().getOpenedProbeSessions().put(probeInventory, session);

        // 打开自定义背包给执行命令的玩家
        clicker.openInventory(probeInventory);

        clicker.sendMessage(ChatColor.GREEN + "你已打开 " + targetPlayer.getName() + " 的背包。");

        // <<<<<<<<<<<<<<<<<<<<<<<<<<<< 启动实时更新任务 >>>>>>>>>>>>>>>>>>>>>>>>>>
        // 调用 PocketProbe 实例中的方法来启动刷新任务
        PocketProbe.getInstance().startProbeRefreshTask(session);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    }

    /**
     * Handles the custom inventory close event, synchronizing items back to the target player's real inventory.
     * Also cancels the real-time update task.
     * @param event The inventory close event.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Check if the closed inventory is one of our custom probe inventories.
        Inventory closedInventory = event.getInventory();
        Map<Inventory, ProbeSession> openedSessions = PocketProbe.getInstance().getOpenedProbeSessions();

        if (openedSessions.containsKey(closedInventory)) {
            ProbeSession session = openedSessions.get(closedInventory);
            Player targetPlayer = session.getTargetPlayer();
            PlayerInventory targetInv = targetPlayer.getInventory();

            // <<<<<<<<<<<<<<<<<<<<<<<<<<<< Cancel Real-time Update Task >>>>>>>>>>>>>>>>>>>>>>>>>>
            // If a refresh task exists, cancel it.
            if (session.getRefreshTask() != null) {
                session.getRefreshTask().cancel();
            }
            // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

            // Remove the entry from the Map, indicating this session has ended.
            openedSessions.remove(closedInventory);

            // Synchronize armor slots.
            targetInv.setHelmet(closedInventory.getItem(0));
            targetInv.setChestplate(closedInventory.getItem(1));
            targetInv.setLeggings(closedInventory.getItem(2));
            targetInv.setBoots(closedInventory.getItem(3));

            // Synchronize off-hand.
            targetInv.setItemInOffHand(closedInventory.getItem(8));

            // Synchronize main inventory and hotbar (total 36 slots).
            ItemStack[] newStorageContents = new ItemStack[36];
            for (int i = 0; i < 9; i++) { // Hotbar (custom GUI slots 45-53 -> player inventory slots 0-8)
                newStorageContents[i] = closedInventory.getItem(45 + i);
            }
            for (int i = 0; i < 27; i++) { // Main inventory (custom GUI slots 18-44 -> player inventory slots 9-35)
                newStorageContents[9 + i] = closedInventory.getItem(18 + i);
            }
            targetInv.setStorageContents(newStorageContents);

            // Ensure the player's inventory is updated.
            targetPlayer.updateInventory();

            // Optionally send a message to the player who opened the inventory.
            event.getPlayer().sendMessage(ChatColor.GREEN + targetPlayer.getName() + " 的背包已更新。");
        }
    }

    /**
     * Prevents players from moving or picking up filler items in the probe inventory.
     * @param event The inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Map<Inventory, ProbeSession> openedSessions = PocketProbe.getInstance().getOpenedProbeSessions();
        Inventory currentOpenInventory = event.getInventory();

        // Only process if the currently open inventory is one of our custom probe inventories.
        if (openedSessions.containsKey(currentOpenInventory)) {
            int slot = event.getRawSlot(); // Get the raw slot clicked.

            // Check if the clicked slot is a filler slot (4-7, 9-17).
            int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
            boolean isFillerSlot = false;
            for (int fillerSlot : fillerSlots) {
                if (slot == fillerSlot) {
                    isFillerSlot = true;
                    break;
                }
            }

            if (isFillerSlot) {
                // Prevent players from picking up filler items or placing other items into filler slots.
                // Prevent picking up filler.
                if (currentOpenInventory.getItem(slot) != null && currentOpenInventory.getItem(slot).getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    event.setCancelled(true);
                }
                // Prevent placing items into filler slots (if there's an item on the cursor and it's not air).
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                }
            }
        }
    }
}