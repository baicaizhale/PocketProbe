package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * This class handles the /pocketprobe command.
 * It implements CommandExecutor, which requires the onCommand method to be overridden.
 */
public class PocketProbeCommand implements CommandExecutor {

    /**
     * This method is called whenever the registered command (in this case, /pocketprobe) is executed.
     *
     * @param sender The entity that executed the command (e.g., a player, console, or command block).
     * @param command The command object itself.
     * @param label The actual command alias used (e.g., "pocketprobe" if typed "/pocketprobe").
     * @param args An array of arguments provided after the command (e.g., in "/pocketprobe Notch", "Notch" is an arg).
     * @return true if the command was handled successfully, false otherwise (which will display the command's usage message).
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Check if the command sender is a player.
        // The plugin's functionality involves opening an inventory, which only players can do.
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            // Return true to indicate the command was handled, even if it failed due to wrong sender type.
            return true;
        }

        // Cast the sender to a Player object for player-specific operations.
        Player player = (Player) sender;

        // 2. Check if the player has the necessary permission.
        // Permissions are defined in plugin.yml and can be managed by server admins.
        if (!player.hasPermission("pocketprobe.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // 3. Validate command arguments.
        // We expect one argument: the target player's name.
        if (args.length == 0) {
            // If no arguments are provided, send a usage message.
            player.sendMessage(ChatColor.YELLOW + "Usage: /pocketprobe <player_name>");
            return true;
        }

        // Get the target player's name from the first argument.
        String targetPlayerName = args[0];

        // 4. Find the target player.
        // Bukkit.getPlayerExact(name) tries to find an online player with the exact given name.
        // This is generally preferred for commands that require an active player.
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        // 5. Check if the target player is online and exists.
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player '" + targetPlayerName + "' is not online or does not exist.");
            return true;
        }

        // 6. Get the target player's inventory.
        // Player.getInventory() returns the PlayerInventory object for the player.
        Inventory targetInventory = targetPlayer.getInventory();

        // 7. Open the target inventory for the command sender.
        // This is the core functionality: the sender will see and be able to interact with
        // the target player's inventory as if it were their own. Changes made here will affect the target player.
        player.openInventory(targetInventory);

        // 8. Send a success message to the command sender.
        player.sendMessage(ChatColor.GREEN + "Opened " + targetPlayer.getName() + "'s inventory.");

        // Return true to indicate that the command was successfully processed.
        return true;
    }
}
