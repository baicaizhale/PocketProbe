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
 * 此类监听玩家事件，特别是右键点击其他实体（玩家）和自定义探查背包的关闭事件。
 * 它实现了 Listener 接口，这是 Spigot 事件监听器所必需的。
 */
public class PocketProbeListener implements Listener {

    /**
     * 当 PlayerInteractAtEntityEvent 发生时，Spigot 会自动调用此方法。
     * @param event 包含玩家交互详细信息的事件对象。
     */
    @EventHandler
    public void onPlayerRightClickPlayer(PlayerInteractAtEntityEvent event) {
        // 1. 确保事件是由主手触发，以避免副手重复触发。
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // 2. 确保被右键点击的实体是玩家，并使用模式变量 'targetPlayer'。
        if (!(event.getRightClicked() instanceof Player targetPlayer)) {
            return; // 如果不是玩家，则不处理。
        }

        // 3. 获取执行右键点击操作的玩家（查看者）。
        Player clicker = event.getPlayer(); // 右键点击的玩家。

        // 4. 检查点击者是否拥有必要的权限。
        if (!clicker.hasPermission("pocketprobe.use")) {
            clicker.sendMessage(ChatColor.RED + "你没有权限使用此功能。");
            return;
        }

        // 5. 取消事件，防止默认的右键行为（例如，如果手中持有物品，可能会对目标玩家使用物品）。
        event.setCancelled(true);

        // 创建一个 9x6 (54格) 的自定义背包，用于显示所有物品。
        Inventory probeInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "查看 " + targetPlayer.getName() + " 的背包");

        PlayerInventory targetInv = targetPlayer.getInventory();

        // 填充主物品栏 (27格) 和热启动栏 (9格)，共 36 格。
        ItemStack[] storageContents = targetInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            // 0-8 是热启动栏，9-35 是主物品栏。
            // 在自定义 GUI 中，我们希望热启动栏在最底部 (槽位 45-53)，主物品栏在其上方 (槽位 18-44)。
            if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> 自定义 GUI 槽位 45-53)
                probeInventory.setItem(45 + i, storageContents[i]);
            } else { // 主物品栏 (玩家背包槽位 9-35 -> 自定义 GUI 槽位 18-44)
                probeInventory.setItem(18 + (i - 9), storageContents[i]);
            }
        }

        // 放置盔甲栏 (自定义 GUI 顶部 4 格)
        probeInventory.setItem(0, targetInv.getHelmet());      // 头盔
        probeInventory.setItem(1, targetInv.getChestplate());  // 胸甲
        probeInventory.setItem(2, targetInv.getLeggings());    // 护腿
        probeInventory.setItem(3, targetInv.getBoots());       // 靴子

        // 放置副手物品 (自定义 GUI 右上方槽位 8)
        probeInventory.setItem(8, targetInv.getItemInOffHand()); // 副手

        // 填充空槽位，使其看起来更整洁。
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " "); // 设置为空名称以隐藏物品名。
            filler.setItemMeta(fillerMeta);
        }

        // 优化填充物放置逻辑，避免重复代码段。
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : fillerSlots) {
            probeInventory.setItem(slot, filler);
        }

        // 创建新的探查会话。
        ProbeSession session = new ProbeSession(targetPlayer, probeInventory, clicker);

        // 将自定义背包和会话关联起来，以便在背包关闭时进行同步。
        PocketProbe.getInstance().getOpenedProbeSessions().put(probeInventory, session);

        // 打开自定义背包给执行命令的玩家。
        clicker.openInventory(probeInventory);

        clicker.sendMessage(ChatColor.GREEN + "你已打开 " + targetPlayer.getName() + " 的背包。");

        // <<<<<<<<<<<<<<<<<<<<<<<<<<<< 启动实时更新任务 >>>>>>>>>>>>>>>>>>>>>>>>>>
        // 调用 PocketProbe 实例中的方法来启动刷新任务。
        PocketProbe.getInstance().startProbeRefreshTask(session);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    }

    /**
     * 处理自定义背包的关闭事件，将物品同步回目标玩家的真实背包。
     * 同时取消实时更新任务。
     * @param event 背包关闭事件。
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 检查关闭的背包是否是我们的自定义探查背包。
        Inventory closedInventory = event.getInventory();
        Map<Inventory, ProbeSession> openedSessions = PocketProbe.getInstance().getOpenedProbeSessions();

        if (openedSessions.containsKey(closedInventory)) {
            ProbeSession session = openedSessions.get(closedInventory);
            Player targetPlayer = session.getTargetPlayer(); // 修复：确保调用的是 getTargetPlayer()
            PlayerInventory targetInv = targetPlayer.getInventory();

            // <<<<<<<<<<<<<<<<<<<<<<<<<<<< 取消实时更新任务 >>>>>>>>>>>>>>>>>>>>>>>>>>
            // 如果存在刷新任务，取消它。
            if (session.getRefreshTask() != null) {
                session.getRefreshTask().cancel();
            }
            // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

            // 移除 Map 中的条目，表示此会话已结束。
            openedSessions.remove(closedInventory);

            // 同步盔甲栏。
            targetInv.setHelmet(closedInventory.getItem(0));
            targetInv.setChestplate(closedInventory.getItem(1));
            targetInv.setLeggings(closedInventory.getItem(2));
            targetInv.setBoots(closedInventory.getItem(3));

            // 同步副手。
            targetInv.setItemInOffHand(closedInventory.getItem(8));

            // 同步主物品栏和热启动栏（共 36 格）。
            ItemStack[] newStorageContents = new ItemStack[36];
            for (int i = 0; i < 9; i++) { // 热启动栏 (自定义 GUI 槽位 45-53 -> 玩家背包槽位 0-8)
                newStorageContents[i] = closedInventory.getItem(45 + i);
            }
            for (int i = 0; i < 27; i++) { // 主物品栏 (自定义 GUI 槽位 18-44 -> 玩家背包槽位 9-35)
                newStorageContents[9 + i] = closedInventory.getItem(18 + i);
            }
            targetInv.setStorageContents(newStorageContents);

            // 确保玩家背包已更新。
            targetPlayer.updateInventory();

            // （可选）向打开背包的玩家发送消息。
            event.getPlayer().sendMessage(ChatColor.GREEN + targetPlayer.getName() + " 的背包已更新。");
        }
    }

    /**
     * 防止玩家在探查背包中移动或拿起填充物。
     * 同时实现操作者在探查背包中的操作实时同步到目标玩家的实际背包。
     * @param event 背包点击事件。
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Map<Inventory, ProbeSession> openedSessions = PocketProbe.getInstance().getOpenedProbeSessions();
        Inventory clickedInventory = event.getClickedInventory(); // 获取被点击的背包

        // 只有当点击的背包是我们自定义的探查背包时才进行处理。
        if (clickedInventory != null && openedSessions.containsKey(clickedInventory)) {
            int slot = event.getRawSlot(); // 获取点击的原始槽位。
            ProbeSession session = openedSessions.get(clickedInventory);
            Player targetPlayer = session.getTargetPlayer(); // 获取目标玩家

            // 检查点击的槽位是否是填充物槽位 (4-7, 9-17)。
            int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
            boolean isFillerSlot = false;
            for (int fillerSlot : fillerSlots) {
                if (slot == fillerSlot) {
                    isFillerSlot = true;
                    break;
                }
            }

            if (isFillerSlot) {
                // 阻止玩家捡起填充物或将其他物品放入填充物槽位。
                event.setCancelled(true);
            } else {
                // ****** 关键修复：实时同步操作者在探查背包中的操作到目标玩家的实际背包 ******
                // 这个逻辑会在操作者点击/拖动物品时立即尝试同步。
                // 延迟执行以确保 Bukkit 自己的 InventoryClickEvent 处理完成后，再获取并同步最新状态。
                Bukkit.getScheduler().runTaskLater(PocketProbe.getInstance(), () -> {
                    // 再次检查会话是否存在，以防在延迟执行期间背包被关闭。
                    if (!targetPlayer.isOnline() || !openedSessions.containsKey(clickedInventory)) {
                        return;
                    }

                    PlayerInventory targetInv = targetPlayer.getInventory();
                    ItemStack currentItemInProbe = clickedInventory.getItem(slot);

                    // 根据槽位类型进行同步
                    if (slot >= 0 && slot <= 3) { // 盔甲槽
                        if (slot == 0) targetInv.setHelmet(currentItemInProbe);
                        else if (slot == 1) targetInv.setChestplate(currentItemInProbe);
                        else if (slot == 2) targetInv.setLeggings(currentItemInProbe);
                        else if (slot == 3) targetInv.setBoots(currentItemInProbe);
                    } else if (slot == 8) { // 副手槽
                        targetInv.setItemInOffHand(currentItemInProbe);
                    } else if (slot >= 18 && slot <= 44) { // 主物品栏 (对应玩家背包槽位 9-35)
                        targetInv.setItem(slot - 18 + 9, currentItemInProbe);
                    } else if (slot >= 45 && slot <= 53) { // 热启动栏 (对应玩家背包槽位 0-8)
                        targetInv.setItem(slot - 45, currentItemInProbe);
                    }

                    // 强制更新目标玩家的客户端背包显示
                    targetPlayer.updateInventory();

                }, 1L); // 延迟 1 tick 执行，确保 Spigot 内部的点击处理完成。
            }
        }
    }
}