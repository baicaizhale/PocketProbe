package org.YanPl.pocketProbe;

import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics; // 导入 bStats Metrics 类

import java.util.Objects;

/**
 * The main class for the PocketProbe Spigot plugin.
 * This class extends JavaPlugin, which is the base class for all Spigot plugins.
 */
public final class PocketProbe extends JavaPlugin {

    /**
     * This method is called when the plugin is enabled (i.e., when the server starts or the plugin is loaded).
     */
    @Override
    public void onEnable() {
        // Log a message to the server console to indicate that the plugin has started.
        // getLogger() provides a standard way to log messages in Spigot.
        getLogger().info("PocketProbe has been enabled!");

        // 初始化 bStats 统计服务
        int pluginId = 26275; // 替换为你的插件的实际 bStats ID
        Metrics metrics = new Metrics(this, pluginId);


        // 创建命令执行器的实例
        PocketProbeCommand commandExecutor = new PocketProbeCommand();

        // 注册命令执行器 for the "pocketprobe" command
        // 当玩家输入 "/pocketprobe" 或 "/pp" 时，PocketProbeCommand 的 onCommand 方法会被调用。
        Objects.requireNonNull(this.getCommand("pocketprobe")).setExecutor(commandExecutor);

        // <<<<<<<<<<<<<<<< 新增的 Tab Completer 注册行 >>>>>>>>>>>>>>>>>
        // 注册 Tab 补全器 for the "pocketprobe" command
        // 当玩家输入 "/pocketprobe " 或 "/pp " 后按 Tab 键时，PocketProbeCommand 的 onTabComplete 方法会被调用。
        Objects.requireNonNull(this.getCommand("pocketprobe")).setTabCompleter(commandExecutor);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    }

    /**
     * This method is called when the plugin is disabled (i.e., when the server stops or the plugin is unloaded).
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("PocketProbe has been disabled!");
    }
}