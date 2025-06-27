package org.YanPl.pocketProbe;

import org.bukkit.inventory.Inventory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        Metrics metrics = new Metrics(this, pluginId);

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
}
