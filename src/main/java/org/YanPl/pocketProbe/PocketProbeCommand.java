package org.YanPl.pocketProbe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class handles the /pocketprobe command and provides tab completion for player names.
 * It implements CommandExecutor for command handling and TabCompleter for tab completion.
 */
public class PocketProbeCommand implements CommandExecutor, TabCompleter {

    private final PocketProbe plugin; // 存储插件实例

    // 构造函数，接收插件实例
    public PocketProbeCommand(PocketProbe plugin) {
        this.plugin = plugin;
    }

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
        // 1. Check if the command sender is a player，并使用模式变量 'player'
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        // 2. Check if the player has the necessary permission.
        if (!player.hasPermission("pocketprobe.use")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        // 3. Validate command arguments.
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "用法: /" + label + " <玩家名>");
            return true;
        }

        String targetPlayerName = args[0];

        // 4. Find the target player.
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        // 5. Check if the target player is online and exists.
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(ChatColor.RED + "玩家 '" + targetPlayerName + "' 不在线或不存在。");
            return true;
        }

        // 创建一个 9x6 (54格) 的自定义背包，用于显示所有物品
        Inventory probeInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "查看 " + targetPlayer.getName() + " 的背包");

        PlayerInventory targetInv = targetPlayer.getInventory();

        // 填充主物品栏 (27格) 和热启动栏 (9格)，共 36 格
        ItemStack[] storageContents = targetInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            // 0-8 是热启动栏，9-35 是主物品栏。
            // 我们希望在自定义 GUI 中，热启动栏在最下面 (45-53)，主物品栏在上面 (18-44)。
            if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> 自定义背包槽位 45-53)
                probeInventory.setItem(45 + i, storageContents[i]);
            } else { // 主物品栏 (玩家背包槽位 9-35 -> 自定义背包槽位 18-44)
                probeInventory.setItem(18 + (i - 9), storageContents[i]);
            }
        }

        // 放置盔甲栏 (自定义背包顶部 4 格)
        probeInventory.setItem(0, targetInv.getHelmet());      // 头盔
        probeInventory.setItem(1, targetInv.getChestplate());  // 胸甲
        probeInventory.setItem(2, targetInv.getLeggings());    // 护腿
        probeInventory.setItem(3, targetInv.getBoots());       // 靴子

        // 放置副手 (自定义背包右上方槽位 8)
        probeInventory.setItem(8, targetInv.getItemInOffHand()); // 副手

        // 填充空槽位，使其看起来更整洁
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " "); // 设置为空名称以隐藏物品名
            filler.setItemMeta(fillerMeta);
        }

        // 优化填充物放置逻辑，避免重复代码段
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : fillerSlots) {
            probeInventory.setItem(slot, filler);
        }

        // 修复: 创建 ProbeSession 实例并使用 getOpenedProbeSessions() 方法
        ProbeSession session = new ProbeSession(targetPlayer, probeInventory, player);
        plugin.getOpenedProbeSessions().put(probeInventory, session);

        // 打开自定义背包给执行命令的玩家
        player.openInventory(probeInventory);

        player.sendMessage(ChatColor.GREEN + "已打开 " + targetPlayer.getName() + " 的背包。");

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
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();

            for (String playerName : playerNames) {
                if (playerName.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(playerName);
                }
            }
        }
        return completions;
    }
}
