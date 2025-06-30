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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 处理 /pocketprobe 命令并提供 Tab 补全。
 */
public class PocketProbeCommand implements CommandExecutor, TabCompleter {

    private final PocketProbe plugin;

    /**
     * 构造函数，接收插件实例。
     */
    public PocketProbeCommand(PocketProbe plugin) {
        this.plugin = plugin;
    }

    /**
     * 命令执行时调用。
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用此命令。");
            return true;
        }

        if (!player.hasPermission("pocketprobe.use")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "用法: /" + label + " <玩家名>");
            return true;
        }

        String targetPlayerName = args[0];
        Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(ChatColor.RED + "玩家 '" + targetPlayerName + "' 不在线或不存在。");
            return true;
        }

        Inventory probeInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "查看 " + targetPlayer.getName() + " 的背包");
        PlayerInventory targetInv = targetPlayer.getInventory();

        ItemStack[] storageContents = targetInv.getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            if (i <= 8) { // 热启动栏 (玩家背包槽位 0-8 -> GUI 槽位 45-53)
                probeInventory.setItem(45 + i, storageContents[i]);
            } else { // 主物品栏 (玩家背包槽位 9-35 -> GUI 槽位 18-44)
                probeInventory.setItem(18 + (i - 9), storageContents[i]);
            }
        }

        probeInventory.setItem(0, targetInv.getHelmet());
        probeInventory.setItem(1, targetInv.getChestplate());
        probeInventory.setItem(2, targetInv.getLeggings());
        probeInventory.setItem(3, targetInv.getBoots());
        probeInventory.setItem(8, targetInv.getItemInOffHand());

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.DARK_GRAY + " ");
            filler.setItemMeta(fillerMeta);
        }
        int[] fillerSlots = {4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : fillerSlots) {
            probeInventory.setItem(slot, filler);
        }

        ProbeSession session = new ProbeSession(targetPlayer, probeInventory, player);
        plugin.getOpenedProbeSessions().put(probeInventory, session);

        player.openInventory(probeInventory);
        player.sendMessage(ChatColor.GREEN + "已打开 " + targetPlayer.getName() + " 的背包。");

        plugin.startProbeRefreshTask(session);

        return true;
    }

    /**
     * Tab 补全时调用。
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
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