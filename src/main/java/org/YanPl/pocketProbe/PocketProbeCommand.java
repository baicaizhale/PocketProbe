package org.YanPl.pocketProbe;

import org.bukkit.Bukkit; // Added for Bukkit.getPlayerExact, Bukkit.getOnlinePlayers, Bukkit.createInventory
import org.bukkit.ChatColor; // Added for ChatColor.AQUA, ChatColor.YELLOW, ChatColor.RED, ChatColor.GREEN, ChatColor.DARK_GRAY
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta; // Added for ItemMeta
import org.bukkit.Material; // 关键修复：确保 Material 类被导入

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 此类处理 /pocketprobe 命令并为玩家名称提供 Tab 补全。
 * 它实现了 CommandExecutor 用于命令处理，实现了 TabCompleter 用于 Tab 补全。
 */
public class PocketProbeCommand implements CommandExecutor, TabCompleter {

    private final PocketProbe plugin; // 存储插件实例

    /**
     * 构造函数，接收插件实例。
     * @param plugin PocketProbe 插件的实例。
     */
    public PocketProbeCommand(PocketProbe plugin) {
        this.plugin = plugin;
    }

    /**
     * 每当注册的命令（此处为 /pocketprobe 或 /pp）被执行时调用此方法。
     *
     * @param sender 执行命令的实体（例如，玩家、控制台或命令方块）。
     * @param command 命令对象本身。
     * @param label 实际使用的命令别名（例如，如果输入 "/pocketprobe" 或 "/pp"，则为 "pocketprobe" 或 "pp"）。
     * @param args 命令后提供的参数数组（例如，在 "/pocketprobe Notch" 中，"Notch" 是一个参数）。
     * @return 如果命令处理成功返回 true，否则返回 false（这将显示命令的使用说明）。
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. 检查命令发送者是否为玩家，并使用模式变量 'player'。
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用此命令。");
            return true;
        }

        // 2. 检查玩家是否拥有必要的权限。
        if (!player.hasPermission("pocketprobe.use")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        // 3. 验证命令参数。
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "用法: /" + label + " <玩家名>");
            return true;
        }

        String targetPlayerName = args[0];

        // 4. 查找目标玩家。
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        // 5. 检查目标玩家是否在线且存在。
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(ChatColor.RED + "玩家 '" + targetPlayerName + "' 不在线或不存在。");
            return true;
        }

        // 创建一个 9x6 (54格) 的自定义背包，用于显示所有物品。
        Inventory probeInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "查看 " + targetPlayer.getName() + " 的背包");

        PlayerInventory targetInv = targetPlayer.getInventory();

        // 填充主物品栏 (27格) 和热启动栏 (9格)，共 36 格。
        ItemStack[] storageContents = targetInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            // 0-8 是热启动栏，9-35 是主物品栏。
            // 我们希望在自定义 GUI 中，热启动栏在最底部 (槽位 45-53)，主物品栏在其上方 (槽位 18-44)。
            if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> 自定义 GUI 槽位 45-53)
                probeInventory.setItem(45 + i, storageContents[i]);
            } else { // 主物品栏 (玩家背包槽位 9-35 -> 自定义 GUI 槽位 18-44)
                probeInventory.setItem(18 + (i - 9), storageContents[i]);
            }
        }

        // 放置盔甲栏 (自定义 GUI 顶部 4 格)
        probeInventory.setItem(0, targetInv.getHelmet());      // 头盔
        probeInventory.setItem(1, targetInv.getChestplate());  // 胸甲
        probeInventory.setItem(2, targetInv.getLeggings());    // 护腿
        probeInventory.setItem(3, targetInv.getBoots());       // 靴子

        // 放置副手物品 (自定义 GUI 右上方槽位 8)
        probeInventory.setItem(8, targetInv.getItemInOffHand()); // 副手

        // 填充空槽位，使其看起来更整洁。
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " "); // 设置为空名称以隐藏物品名。
            filler.setItemMeta(fillerMeta);
        }

        // 优化填充物放置逻辑，避免重复代码段
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : fillerSlots) {
            probeInventory.setItem(slot, filler);
        }

        // 创建 ProbeSession 实例
        ProbeSession session = new ProbeSession(targetPlayer, probeInventory, player);
        plugin.getOpenedProbeSessions().put(probeInventory, session);

        // 打开自定义背包给执行命令的玩家
        player.openInventory(probeInventory);

        player.sendMessage(ChatColor.GREEN + "已打开 " + targetPlayer.getName() + " 的背包。");

        // <<<<<<<<<<<<<<<<<<<<<<<<<<<< 启动实时更新任务 >>>>>>>>>>>>>>>>>>>>>>>>>>
        // 调用 PocketProbe 实例中的方法来启动刷新任务
        plugin.startProbeRefreshTask(session);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        return true;
    }

    /**
     * 当玩家在输入命令时按下 TAB 键时调用此方法。
     * 它为命令参数提供建议。
     *
     * @param sender 正在输入命令的实体（通常是玩家）。
     * @param command 命令对象。
     * @param alias 实际使用的命令别名（例如，"pocketprobe" 或 "pp"）。
     * @param args 已经输入的参数。
     * @return 当前参数的建议字符串列表。
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