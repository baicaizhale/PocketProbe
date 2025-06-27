package org.YanPl.pocketProbe;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask; // 导入 BukkitTask 类

/**
 * Represents an active PocketProbe inventory viewing session.
 * Stores references to the target player, the custom probe inventory,
 * the viewer player, and the associated refresh task.
 */
public class ProbeSession {
    private final Player targetPlayer;  // 被查看背包的玩家
    private final Inventory probeInventory; // 自定义的探查背包界面
    private final Player viewerPlayer; // 打开背包的玩家 (查看者)
    private BukkitTask refreshTask;     // 关联的实时刷新任务

    /**
     * 构造一个新的探查会话。
     * @param targetPlayer 被查看背包的玩家。
     * @param probeInventory 自定义的探查背包界面。
     * @param viewerPlayer 打开背包的玩家。
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
