package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bstats.bukkit.Metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * PocketProbe Spigot 插件的主类。
 * 继承 JavaPlugin，是所有 Spigot 插件的基础。
 */
public final class PocketProbe extends JavaPlugin {

    // 存储当前插件的唯一实例
    private static PocketProbe instance;
    // 存储已打开的探查会话
    private final Map<Inventory, ProbeSession> openedProbeSessions = new HashMap<>();

    /**
     * 获取插件的唯一实例。
     */
    public static PocketProbe getInstance() {
        return instance;
    }

    /**
     * 获取存储已打开探查会话的 Map。
     */
    public Map<Inventory, ProbeSession> getOpenedProbeSessions() {
        return openedProbeSessions;
    }

    /**
     * 插件启用时调用。
     */
    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("PocketProbe 已启用！");

        int pluginId = 26275;
        new Metrics(this, pluginId);

        PocketProbeCommand commandExecutor = new PocketProbeCommand(this);
        Objects.requireNonNull(this.getCommand("pocketprobe")).setExecutor(commandExecutor);
        Objects.requireNonNull(this.getCommand("pocketprobe")).setTabCompleter(commandExecutor);
        getServer().getPluginManager().registerEvents(new PocketProbeListener(), this);
    }

    /**
     * 插件禁用时调用。
     */
    @Override
    public void onDisable() {
        for (ProbeSession session : openedProbeSessions.values()) {
            if (session.getRefreshTask() != null) {
                session.getRefreshTask().cancel();
            }
        }
        openedProbeSessions.clear();
        getLogger().info("PocketProbe 已禁用！");
    }

    /**
     * 强制刷新一个探查会话的背包内容。
     * 包含了实际更新背包界面的核心逻辑。
     * @param session 需要刷新的探查会话。
     */
    public void forceRefreshProbe(ProbeSession session) {
        Player targetPlayer = session.getTargetPlayer();
        Inventory probeInventory = session.getProbeInventory();
        Player viewerPlayer = session.getViewerPlayer();

        // 检查会话是否仍然有效
        if (!targetPlayer.isOnline() || viewerPlayer.getOpenInventory() == null || viewerPlayer.getOpenInventory().getTopInventory() != probeInventory) {
            if (session.getRefreshTask() != null) {
                session.getRefreshTask().cancel();
            }
            openedProbeSessions.remove(probeInventory);
            return;
        }

        PlayerInventory latestTargetInv = targetPlayer.getInventory();
        ItemStack[] newProbeContents = new ItemStack[54];

        // 填充盔甲栏 (槽位 0-3)
        newProbeContents[0] = latestTargetInv.getHelmet();
        newProbeContents[1] = latestTargetInv.getChestplate();
        newProbeContents[2] = latestTargetInv.getLeggings();
        newProbeContents[3] = latestTargetInv.getBoots();

        // 填充副手 (槽位 8)
        newProbeContents[8] = latestTargetInv.getItemInOffHand();

        // 填充主物品栏和热启动栏
        ItemStack[] targetStorageContents = latestTargetInv.getStorageContents();
        for (int i = 0; i < targetStorageContents.length; i++) {
            if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> 探查背包槽位 45-53)
                newProbeContents[45 + i] = targetStorageContents[i];
            } else { // 主物品栏 (玩家背包槽位 9-35 -> 探查背包槽位 18-44)
                newProbeContents[18 + (i - 9)] = targetStorageContents[i];
            }
        }

        // 填充空槽位（灰色玻璃板）
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " ");
            filler.setItemMeta(fillerMeta);
        }
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : fillerSlots) {
            newProbeContents[slot] = filler;
        }

        // 强制更新整个探查背包的内容
        probeInventory.setContents(newProbeContents);
        viewerPlayer.updateInventory();
    }

    /**
     * 启动一个 BukkitRunnable 任务，用于定期刷新探查背包的内容。
     * @param session 当前的探查会话。
     */
    public void startProbeRefreshTask(ProbeSession session) {
        BukkitTask refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                forceRefreshProbe(session);
            }
        }.runTaskTimer(this, 0L, 2L); // 每 0.1 秒执行一次 (2 ticks)

        session.setRefreshTask(refreshTask);
    }
}