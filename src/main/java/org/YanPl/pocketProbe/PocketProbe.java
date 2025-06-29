package org.YanPl.pocketProbe;

import org.bukkit.Bukkit; // Added for Bukkit.getScheduler().runTaskLater, etc.
import org.bukkit.ChatColor; // Added for ChatColor.AQUA, ChatColor.DARK_GRAY
import org.bukkit.Material;   // Added for Material.GRAY_STAINED_GLASS_PANE
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta; // Added for ItemMeta
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bstats.bukkit.Metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

                // ***** 关键修复：重新构建整个探查背包的内容数组 *****
                ItemStack[] newProbeContents = new ItemStack[54];

                // 填充盔甲栏 (槽位 0-3)
                newProbeContents[0] = latestTargetInv.getHelmet();
                newProbeContents[1] = latestTargetInv.getChestplate();
                newProbeContents[2] = latestTargetInv.getLeggings();
                newProbeContents[3] = latestTargetInv.getBoots();

                // 填充副手 (槽位 8)
                newProbeContents[8] = latestTargetInv.getItemInOffHand();

                // 填充主物品栏 (玩家背包槽位 9-35 -> 探查背包槽位 18-44)
                // 填充热启动栏 (玩家背包槽位 0-8 -> 探查背包槽位 45-53)
                ItemStack[] targetStorageContents = latestTargetInv.getStorageContents();
                for (int i = 0; i < targetStorageContents.length; i++) {
                    if (i <= 8) { // 热启动栏
                        newProbeContents[45 + i] = targetStorageContents[i];
                    } else { // 主物品栏
                        newProbeContents[18 + (i - 9)] = targetStorageContents[i];
                    }
                }

                // 填充空槽位（灰色玻璃板），确保它们不被玩家物品覆盖，并保持界面整洁。
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta fillerMeta = filler.getItemMeta();
                if (fillerMeta != null) {
                    fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " "); // 设置为空名称以隐藏物品名。
                    filler.setItemMeta(fillerMeta);
                }
                int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
                for (int slot : fillerSlots) {
                    // 只有当该槽位目前是空的，或者已经被填充物占据时，才重新填充
                    // 这避免了覆盖玩家在探查界面中可能放置的物品
                    ItemStack currentProbeItem = probeInventory.getItem(slot);
                    // 检查 currentProbeItem 是否为 null，以防止 NPE
                    if (currentProbeItem == null || (currentProbeItem.getType() == Material.GRAY_STAINED_GLASS_PANE && Objects.equals(currentProbeItem.getItemMeta(), fillerMeta))) {
                        newProbeContents[slot] = filler;
                    } else {
                        // 如果玩家在此槽位放置了非填充物，则保留玩家放置的物品
                        newProbeContents[slot] = currentProbeItem;
                    }
                }
                // ***** 结束重新构建 *****

                // 使用 setContents 强制更新整个探查背包的内容，确保客户端完全同步。
                probeInventory.setContents(newProbeContents);

                // 强制查看者刷新其客户端的背包界面，以确保最及时的显示。
                viewerPlayer.updateInventory();
            }

        }.runTaskTimer(this, 0L, 2L); // 0L: 立即开始, 2L: 每 2 tick 执行一次 (0.1秒)

        // 将任务关联到会话
        session.setRefreshTask(refreshTask);
    }
}