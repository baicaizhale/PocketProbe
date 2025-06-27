package org.YanPl.pocketProbe;

import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics; // Import bStats Metrics class

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
        getLogger().info("PocketProbe has been enabled!");

        // Initialize bStats statistics service
        int pluginId = 26275; // Replace with your plugin's actual bStats ID
        Metrics metrics = new Metrics(this, pluginId);

        // Create an instance of the command executor.
        PocketProbeCommand commandExecutor = new PocketProbeCommand();

        // Register the command executor for the "pocketprobe" command.
        Objects.requireNonNull(this.getCommand("pocketprobe")).setExecutor(commandExecutor);

        // Register the Tab Completer for the "pocketprobe" command.
        Objects.requireNonNull(this.getCommand("pocketprobe")).setTabCompleter(commandExecutor);

        // <<<<<<<<<<<<<<<< 新增事件监听器注册 >>>>>>>>>>>>>>>>>
        // Register the event listener so the plugin can respond to player interaction events.
        // 'this' refers to the current plugin instance, and PocketProbeListener is the class that handles the events.
        getServer().getPluginManager().registerEvents(new PocketProbeListener(), this);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
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
