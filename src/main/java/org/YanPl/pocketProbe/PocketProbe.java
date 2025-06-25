package org.YanPl.pocketProbe;

import org.bukkit.plugin.java.JavaPlugin;

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

        // Register the command executor for the "pocketprobe" command.
        // When a player types "/pocketprobe", the onCommand method in PocketProbeCommand will be called.
        // 'this' refers to an instance of PocketProbe, which is passed to the command executor
        // so it can access plugin-specific methods or settings if needed (though not strictly necessary for this simple plugin).
        this.getCommand("pocketprobe").setExecutor(new PocketProbeCommand());
    }

    /**
     * This method is called when the plugin is disabled (i.e., when the server stops or the plugin is unloaded).
     */
    @Override
    public void onDisable() {
        // Log a message to the server console to indicate that the plugin is shutting down.
        getLogger().info("PocketProbe has been disabled!");
    }
}
