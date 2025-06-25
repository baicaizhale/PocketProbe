package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter; // 导入 TabCompleter 接口
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList; // 导入 ArrayList
import java.util.List; // 导入 List
import java.util.stream.Collectors; // 导入 Collectors

/**
 * This class handles the /pocketprobe command and provides tab completion for player names.
 * It implements CommandExecutor for command handling and TabCompleter for tab completion.
 */
public class PocketProbeCommand implements CommandExecutor, TabCompleter { // 实现 TabCompleter 接口

    /**
     * This method is called whenever the registered command (in this case, /pocketprobe or /pp) is executed.
     *
     * @param sender The entity that executed the command (e.g., a player, console, or command block).
     * @param command The command object itself.
     * @param label The actual command alias used (e.g., "pocketprobe" or "pp" if typed "/pocketprobe" or "/pp").
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
            player.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <player_name>"); // 使用 label 确保提示正确
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

    /**
     * This method is called when a player presses TAB while typing a command.
     * It provides suggestions for command arguments.
     *
     * @param sender The entity that is typing the command (usually a Player).
     * @param command The command object.
     * @param alias The command alias used (e.g., "pocketprobe" or "pp").
     * @param args The arguments typed so far.
     * @return A list of suggested strings for the current argument.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Create an empty list to store suggestions.
        List<String> completions = new ArrayList<>();

        // We only care about tab completion for the first argument (player name).
        if (args.length == 1) {
            // Get all online player names.
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName) // Map Player objects to their names (Strings)
                    .collect(Collectors.toList()); // Collect them into a List

            // Filter player names based on what the user has typed so far (args[0]).
            // This makes the tab completion dynamic.
            for (String playerName : playerNames) {
                if (playerName.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(playerName);
                }
            }
        }
        // If args.length is not 1 (e.g., no arguments or more than one), return an empty list,
        // meaning no suggestions are provided by this plugin.
        return completions;
    }
}
