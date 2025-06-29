package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

/**
 * Represents a probe session that tracks the target player, viewer player,
 * and the custom inventory being used for probing.
 */
public class ProbeSession {
    private final Player targetPlayer;
    private final Inventory probeInventory;
    private final Player viewerPlayer;
    private BukkitTask refreshTask;

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
