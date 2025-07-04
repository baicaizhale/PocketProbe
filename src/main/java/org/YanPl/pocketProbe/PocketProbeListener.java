package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;


/**
 * 监听玩家事件（右键点击实体、背包关闭、背包点击、拖动、拾取、丢弃、消耗、合成等）。
 */
public class PocketProbeListener implements Listener {

    /**
     * 立即刷新所有正在探查指定目标玩家背包的探查会话。
     * 该方法会确保目标玩家的背包状态在事件处理完成后得到最终更新后立即同步。
     * @param target 目标玩家，其背包发生了改变。
     */
    private void immediateRefreshProbesForTarget(Player target) {
        Bukkit.getScheduler().runTaskLater(PocketProbe.getInstance(), () -> {
            for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
                if (session.getTargetPlayer().equals(target)) {
                    PocketProbe.getInstance().forceRefreshProbe(session);
                }
            }
        }, 1L); // 延迟 1 tick，确保目标玩家背包状态已完全更新后再刷新探查界面。
    }

    /**
     * 处理玩家右键点击实体事件。
     */
    @EventHandler
    public void onPlayerRightClickPlayer(@NotNull PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player targetPlayer)) {
            return;
        }

        Player clicker = event.getPlayer();

        if (!clicker.hasPermission("pocketprobe.use")) {
            clicker.sendMessage(ChatColor.RED + "你没有权限使用此功能。");
            return;
        }

        event.setCancelled(true);

        Inventory probeInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "查看 " + targetPlayer.getName() + " 的背包");
        PlayerInventory targetInv = targetPlayer.getInventory();

        ItemStack[] storageContents = targetInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> GUI 槽位 45-53)
                probeInventory.setItem(45 + i, storageContents[i]);
            } else { // 主物品栏 (玩家背包槽位 9-35 -> GUI 槽位 18-44)
                probeInventory.setItem(18 + (i - 9), storageContents[i]);
            }
        }

        probeInventory.setItem(0, targetInv.getHelmet());
        probeInventory.setItem(1, targetInv.getChestplate());
        probeInventory.setItem(2, targetInv.getLeggings());
        probeInventory.setItem(3, targetInv.getBoots());
        probeInventory.setItem(8, targetInv.getItemInOffHand());

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " ");
            filler.setItemMeta(fillerMeta);
        }
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : fillerSlots) {
            probeInventory.setItem(slot, filler);
        }

        ProbeSession session = new ProbeSession(targetPlayer, probeInventory, clicker);
        PocketProbe.getInstance().getOpenedProbeSessions().put(probeInventory, session);

        clicker.openInventory(probeInventory);
        clicker.sendMessage(ChatColor.GREEN + "你已打开 " + targetPlayer.getName() + " 的背包。");

        PocketProbe.getInstance().startProbeRefreshTask(session);
    }

    /**
     * 处理自定义探查背包关闭事件。
     * 关键修复：移除关闭时向目标玩家背包的写回逻辑，避免覆盖。
     */
    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        Inventory closedInventory = event.getInventory();
        Map<Inventory, ProbeSession> openedSessions = PocketProbe.getInstance().getOpenedProbeSessions();

        if (openedSessions.containsKey(closedInventory)) {
            ProbeSession session = openedSessions.get(closedInventory);

            // 取消实时更新任务
            if (session.getRefreshTask() != null) {
                session.getRefreshTask().cancel();
            }
            // 从会话中移除
            openedSessions.remove(closedInventory);

            // 核心修复：移除所有向目标玩家背包写回的逻辑
            // 因为操作者对探查界面的操作已经实时同步到目标玩家背包，
            // 且目标玩家的自身背包变化也会实时更新探查界面。
            // 此时，目标玩家的背包已经是最新状态，无需再次写回，以避免覆盖。

            event.getPlayer().sendMessage(ChatColor.GREEN + session.getTargetPlayer().getName() + " 的背包探查已关闭。");
        }
    }

    /**
     * 处理查看者在探查背包中的点击事件。
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProbeInventoryClick(@NotNull InventoryClickEvent event) {
        Map<Inventory, ProbeSession> openedSessions = PocketProbe.getInstance().getOpenedProbeSessions();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && openedSessions.containsKey(clickedInventory)) {
            int slot = event.getRawSlot();
            ProbeSession session = openedSessions.get(clickedInventory);
            Player targetPlayer = session.getTargetPlayer();

            int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
            boolean isFillerSlot = false;
            for (int fillerSlot : fillerSlots) {
                if (slot == fillerSlot) {
                    isFillerSlot = true;
                    break;
                }
            }

            if (isFillerSlot) {
                event.setCancelled(true);
            } else {
                // 实时同步操作者在探查背包中的操作到目标玩家的实际背包
                Bukkit.getScheduler().runTaskLater(PocketProbe.getInstance(), () -> {
                    if (!targetPlayer.isOnline() || !openedSessions.containsKey(clickedInventory)) {
                        return;
                    }

                    PlayerInventory targetInv = targetPlayer.getInventory();
                    ItemStack currentItemInProbe = clickedInventory.getItem(slot);

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

                    targetPlayer.updateInventory();
                }, 1L); // 延迟 1 tick 执行，确保 Spigot 内部点击处理完成。
            }
        }
    }

    /**
     * 监听目标玩家自身背包中的点击事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetInventoryClick(@NotNull InventoryClickEvent event) {
        // 确保是目标玩家自己操作自己的背包，而不是探查界面
        if (!(event.getWhoClicked() instanceof Player targetPlayer) ||
                PocketProbe.getInstance().getOpenedProbeSessions().containsKey(event.getInventory())) {
            return;
        }

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家自身背包中的拖动事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetInventoryDrag(@NotNull InventoryDragEvent event) {
        // 确保是目标玩家自己操作自己的背包，而不是探查界面
        if (!(event.getWhoClicked() instanceof Player targetPlayer) ||
                PocketProbe.getInstance().getOpenedProbeSessions().containsKey(event.getInventory())) {
            return;
        }

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家拾取物品事件 (使用新的EntityPickupItemEvent)。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetPlayerPickupItem(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player targetPlayer)) {
            return;
        }

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家丢弃物品事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        Player targetPlayer = event.getPlayer();

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家消耗物品事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        Player targetPlayer = event.getPlayer();

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家合成物品事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetCraftItem(@NotNull CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player targetPlayer)) {
            return;
        }

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家物品损坏事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetPlayerItemBreak(@NotNull PlayerItemBreakEvent event) {
        Player targetPlayer = event.getPlayer();

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家物品受损事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetPlayerItemDamage(@NotNull PlayerItemDamageEvent event) {
        Player targetPlayer = event.getPlayer();

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家交换主副手物品事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetPlayerSwapHandItems(@NotNull PlayerSwapHandItemsEvent event) {
        Player targetPlayer = event.getPlayer();

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }

    /**
     * 监听目标玩家切换手持物品事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTargetPlayerItemHeld(@NotNull PlayerItemHeldEvent event) {
        Player targetPlayer = event.getPlayer();

        // 检查是否有人在探查这个玩家的背包
        boolean isBeingProbed = false;
        for (ProbeSession session : PocketProbe.getInstance().getOpenedProbeSessions().values()) {
            if (session.getTargetPlayer().equals(targetPlayer)) {
                isBeingProbed = true;
                break;
            }
        }

        if (isBeingProbed) {
            // 切换手持物品不会改变背包内容，但会改变显示的选中槽位
            // 我们可以选择是否需要刷新，这里先保留以防万一
            immediateRefreshProbesForTarget(targetPlayer);
        }
    }
}