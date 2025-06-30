package org.YanPl.pocketProbe;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

/**
 * 表示一个活动的 PocketProbe 背包查看会话。
 */
public class ProbeSession {
    private final Player targetPlayer;
    private final Inventory probeInventory;
    private final Player viewerPlayer;
    private BukkitTask refreshTask;

    /**
     * 构造一个新的探查会话。
     */
    public ProbeSession(Player targetPlayer, Inventory probeInventory, Player viewerPlayer) {
        this.targetPlayer = targetPlayer;
        this.probeInventory = probeInventory;
        this.viewerPlayer = viewerPlayer;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public Inventory getProbeInventory() {
        return probeInventory;
    }

    public Player getViewerPlayer() {
        return viewerPlayer;
    }

    public BukkitTask getRefreshTask() {
        return refreshTask;
    }

    public void setRefreshTask(BukkitTask refreshTask) {
        this.refreshTask = refreshTask;
    }
}