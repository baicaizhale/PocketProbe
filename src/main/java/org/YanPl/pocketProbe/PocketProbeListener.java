package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;


/**
 * 监听玩家事件（右键点击实体、背包关闭、背包点击、拖动、拾取、丢弃、消耗、合成）。
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
        }, 0L); // 延迟 0 tick，立即运行。
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

            // ****** 核心修复：移除以下所有向目标玩家背包写回的逻辑 ******
            // 因为操作者对探查界面的操作已经实时同步到目标玩家背包，
            // 且目标玩家的自身背包变化也会实时更新探查界面。
            // 此时，目标玩家的背包已经是最新状态，无需再次写回，以避免覆盖。
            // targetInv.setHelmet(closedInventory.getItem(0));
            // targetInv.setChestplate(closedInventory.getItem(1));
            // targetInv.setLeggings(closedInventory.getItem(2));
            // targetInv.setBoots(closedInventory.getItem(3));
            // targetInv.setItemInOffHand(closedInventory.getItem(8));
            // ...
            // targetInv.setStorageContents(newStorageContents);
            // targetPlayer.updateInventory(); // 这行也可以移除，因为目标玩家的更新由其他事件触发
            // ****** 核心修复结束 ******

            event.getPlayer().sendMessage(ChatColor.GREEN + session.getTargetPlayer().getName() + " 的背包探查已关闭。");
        }
    }

    /**
     * 处理查看者在探查背包中的点击事件。
     */
    @EventHandler
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
    @EventHandler
    public void onTargetInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player targetPlayer) || PocketProbe.getInstance().getOpenedProbeSessions().containsKey(event.getInventory())) {
            return;
        }
        immediateRefreshProbesForTarget(targetPlayer);
    }

    /**
     * 监听目标玩家自身背包中的拖动事件。
     */
    @EventHandler
    public void onTargetInventoryDrag(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player targetPlayer) || PocketProbe.getInstance().getOpenedProbeSessions().containsKey(event.getInventory())) {
            return;
        }
        immediateRefreshProbesForTarget(targetPlayer);
    }

    /**
     * 监听目标玩家拾取物品事件。
     */
    @EventHandler
    public void onTargetPlayerPickupItem(@NotNull PlayerPickupItemEvent event) {
        Player targetPlayer = event.getPlayer();
        immediateRefreshProbesForTarget(targetPlayer);
    }

    /**
     * 监听目标玩家丢弃物品事件。
     */
    @EventHandler
    public void onTargetPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        Player targetPlayer = event.getPlayer();
        immediateRefreshProbesForTarget(targetPlayer);
    }

    /**
     * 监听目标玩家消耗物品事件。
     */
    @EventHandler
    public void onTargetPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        Player targetPlayer = event.getPlayer();
        immediateRefreshProbesForTarget(targetPlayer);
    }

    /**
     * 监听目标玩家合成物品事件。
     */
    @EventHandler
    public void onTargetCraftItem(@NotNull CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player targetPlayer)) {
            return;
        }
        immediateRefreshProbesForTarget(targetPlayer);
    }
}