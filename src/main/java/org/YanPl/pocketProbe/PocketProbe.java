package org.YanPl.pocketProbe;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bstats.bukkit.Metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
// 关键修复：添加 ProbeSession 类的导入
import org.YanPl.pocketProbe.ProbeSession;

/**
 * The main class for the PocketProbe Spigot plugin.
 * This class extends JavaPlugin, which is the base class for all Spigot plugins.
 */
public final class PocketProbe extends JavaPlugin {

    // 存储当前插件的唯一实例，方便其他类访问
    private static PocketProbe instance;

    // 存储已打开的探查会话。
    // key: 自定义背包实例, value: ProbeSession 对象 (包含目标玩家、查看者和刷新任务)
    private final Map<Inventory, ProbeSession> openedProbeSessions = new HashMap<>();

    /**
     * 获取插件的唯一实例。
     * @return PocketProbe 插件实例
     */
    public static PocketProbe getInstance() {
        return instance;
    }

    /**
     * 获取存储已打开探查会话的 Map。
     * @return 存储自定义背包和 ProbeSession 的 Map
     */
    public Map<Inventory, ProbeSession> getOpenedProbeSessions() {
        return openedProbeSessions;
    }

    /**
     * This method is called when the plugin is enabled (i.e., when the server starts or the plugin is loaded).
     */
    @Override
    public void onEnable() {
        // 设置插件实例
        instance = this;

        // Log a message to the server console to indicate that the plugin has started.
        getLogger().info("PocketProbe has been enabled!");

        // Initialize bStats statistics service
        int pluginId = 26275; // Replace with your plugin's actual bStats ID
        new Metrics(this, pluginId); // Removed unused variable 'metrics' by directly creating the object

        // 创建命令执行器的实例，并将插件实例传递给它
        PocketProbeCommand commandExecutor = new PocketProbeCommand(this);

        // 注册命令执行器 for the "pocketprobe" command
        Objects.requireNonNull(this.getCommand("pocketprobe")).setExecutor(commandExecutor);

        // 注册 Tab 补全器 for the "pocketprobe" command
        Objects.requireNonNull(this.getCommand("pocketprobe")).setTabCompleter(commandExecutor);

        // 注册事件监听器，让插件能够响应玩家的交互事件和背包关闭事件
        getServer().getPluginManager().registerEvents(new PocketProbeListener(), this);
    }

    /**
     * This method is called when the plugin is disabled (i.e., when the server stops or the plugin is unloaded).
     * 在插件禁用时，确保停止所有正在运行的实时更新任务，避免资源泄露。
     */
    @Override
    public void onDisable() {
        // 遍历所有打开的探查会话，并取消其刷新任务
        for (ProbeSession session : openedProbeSessions.values()) {
            if (session.getRefreshTask() != null) {
                session.getRefreshTask().cancel();
            }
        }
        // 清空 Map
        openedProbeSessions.clear();

        // Plugin shutdown logic
        getLogger().info("PocketProbe has been disabled!");
    }

    /**
     * 启动一个 BukkitRunnable 任务，用于实时刷新探查背包的内容。
     * @param session 当前的探查会话，包含查看者、目标玩家和自定义背包。
     */
    public void startProbeRefreshTask(ProbeSession session) {
        Player targetPlayer = session.getTargetPlayer();
        Inventory probeInventory = session.getProbeInventory();
        Player viewerPlayer = session.getViewerPlayer();

        BukkitTask refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 如果目标玩家不在线，或者查看者不再查看此背包，则取消任务
                if (!targetPlayer.isOnline() || viewerPlayer.getOpenInventory() == null || viewerPlayer.getOpenInventory().getTopInventory() != probeInventory) {
                    this.cancel();
                    // 如果任务被取消，确保从 Map 中移除会话
                    openedProbeSessions.remove(probeInventory);
                    return;
                }

                // 获取最新的玩家背包内容
                PlayerInventory latestTargetInv = targetPlayer.getInventory();

                // 实时同步盔甲栏
                updateSlot(probeInventory, latestTargetInv.getHelmet(), 0);
                updateSlot(probeInventory, latestTargetInv.getChestplate(), 1);
                updateSlot(probeInventory, latestTargetInv.getLeggings(), 2);
                updateSlot(probeInventory, latestTargetInv.getBoots(), 3);

                // 实时同步副手
                updateSlot(probeInventory, latestTargetInv.getItemInOffHand(), 8);

                // 实时同步主物品栏和热启动栏
                ItemStack[] latestStorageContents = latestTargetInv.getStorageContents();
                for (int i = 0; i < latestStorageContents.length; i++) {
                    if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> 自定义背包槽位 45-53)
                        updateSlot(probeInventory, latestStorageContents[i], 45 + i);
                    } else { // 主物品栏 (玩家背包槽位 9-35 -> 自定义背包槽位 18-44)
                        updateSlot(probeInventory, latestStorageContents[i], 18 + (i - 9));
                    }
                }
            }

            // 辅助方法：仅在物品不同时才更新槽位，减少不必要的更新
            private void updateSlot(Inventory inv, ItemStack newItem, int slot) {
                ItemStack currentItem = inv.getItem(slot);
                if (!Objects.equals(currentItem, newItem)) {
                    inv.setItem(slot, newItem);
                }
            }

        }.runTaskTimer(this, 0L, 2L); // 'this' 指的是 PocketProbe 实例

        // 将任务关联到会话
        session.setRefreshTask(refreshTask);
    }
}