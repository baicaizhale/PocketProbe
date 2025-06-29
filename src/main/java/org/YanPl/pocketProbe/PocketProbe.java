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
import org.YanPl.pocketProbe.ProbeSession;

/**
 * PocketProbe Spigot 插件的主类。
 * 此类继承自 JavaPlugin，它是所有 Spigot 插件的基础类。
 */
public final class PocketProbe extends JavaPlugin {

    // 存储当前插件的唯一实例，方便其他类访问
    private static PocketProbe instance;

    // 存储已打开的探查会话。
    // 键: 自定义背包实例, 值: ProbeSession 对象 (包含目标玩家、查看者和刷新任务)
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
     * 当插件启用时调用此方法（即服务器启动或插件加载时）。
     */
    @Override
    public void onEnable() {
        // 设置插件实例
        instance = this;

        // 向服务器控制台记录消息，指示插件已启动。
        getLogger().info("PocketProbe 已启用！");

        // 初始化 bStats 统计服务
        int pluginId = 26275; // 请替换为您的插件的实际 bStats ID
        new Metrics(this, pluginId); // 直接创建对象，移除了未使用的 'metrics' 变量

        // 创建命令执行器的实例，并将插件实例传递给它
        PocketProbeCommand commandExecutor = new PocketProbeCommand(this);

        // 注册 "pocketprobe" 命令的执行器
        Objects.requireNonNull(this.getCommand("pocketprobe")).setExecutor(commandExecutor);

        // 注册 "pocketprobe" 命令的 Tab 补全器
        Objects.requireNonNull(this.getCommand("pocketprobe")).setTabCompleter(commandExecutor);

        // 注册事件监听器，使插件能够响应玩家的交互事件和背包关闭事件
        getServer().getPluginManager().registerEvents(new PocketProbeListener(), this);
    }

    /**
     * 当插件禁用时调用此方法（即服务器停止或插件卸载时）。
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

        // 插件关闭逻辑
        getLogger().info("PocketProbe 已禁用！");
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
                // 添加对 viewerPlayer.getOpenInventory() 的 null 检查，以避免空指针异常
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
                    if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> 自定义 GUI 槽位 45-53)
                        updateSlot(probeInventory, latestStorageContents[i], 45 + i);
                    } else { // 主物品栏 (玩家背包槽位 9-35 -> 自定义 GUI 槽位 18-44)
                        updateSlot(probeInventory, latestStorageContents[i], 18 + (i - 9));
                    }
                }

                // 强制查看者刷新其客户端的背包界面，以确保最及时的显示。
                viewerPlayer.updateInventory();
            }

            /**
             * 辅助方法：更新指定槽位的物品。
             * 直接设置物品，以确保最及时的客户端刷新，特别是NBT数据可能变化的物品。
             * @param inv 要更新的背包。
             * @param newItem 新的物品堆。
             * @param slot 要更新的槽位。
             */
            private void updateSlot(Inventory inv, ItemStack newItem, int slot) {
                // 不再进行 Objects.equals 检查，直接设置，以确保最及时的客户端刷新。
                // 这种做法可能会增加一些不必要的网络流量，但能确保客户端看到最新状态。
                inv.setItem(slot, newItem);
            }

        }.runTaskTimer(this, 0L, 2L); // 0L: 立即开始, 2L: 每 2 tick 执行一次 (0.1秒)

        // 将任务关联到会话
        session.setRefreshTask(refreshTask);
    }
}