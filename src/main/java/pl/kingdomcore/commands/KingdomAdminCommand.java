package pl.kingdomcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.services.KingdomService;
import pl.kingdomcore.utils.NumberParser;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KingdomAdminCommand implements CommandExecutor, TabCompleter {
    private final KingdomCorePlugin plugin;

    public KingdomAdminCommand(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kingdomcore.admin")) {
            plugin.getMessageService().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> reload(sender);
            case "giveprestige" -> prestige(sender, args, true);
            case "removeprestige" -> prestige(sender, args, false);
            case "setlevel" -> setLevel(sender, args);
            case "delete" -> delete(sender, args);
            case "boss" -> boss(sender, args);
            case "money" -> money(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("kingdomcore.reload")) {
            plugin.getMessageService().send(sender, "no-permission");
            return;
        }
        plugin.reloadConfig();
        plugin.getMessageService().reload();
        plugin.getMessageService().send(sender, "reload");
    }

    private void prestige(CommandSender sender, String[] args, boolean add) {
        if (args.length < 3 || NumberParser.positiveLong(args[2]).isEmpty()) {
            plugin.getMessageService().send(sender, "invalid-number");
            return;
        }
        var optional = plugin.getKingdomService().getByName(args[1]);
        if (optional.isEmpty()) {
            plugin.getMessageService().send(sender, "kingdom.not-found");
            return;
        }
        long amount = NumberParser.positiveLong(args[2]).getAsLong();
        if (add) {
            plugin.getKingdomService().addPrestige(optional.get(), amount, "admin");
            plugin.getMessageService().send(sender, "admin.prestige-added", Map.of("kingdom", optional.get().getName()));
        } else {
            plugin.getKingdomService().removePrestige(optional.get(), amount, "admin");
            plugin.getMessageService().send(sender, "admin.prestige-removed", Map.of("kingdom", optional.get().getName()));
        }
    }

    private void setLevel(CommandSender sender, String[] args) {
        if (args.length < 3 || NumberParser.positiveLong(args[2]).isEmpty()) {
            plugin.getMessageService().send(sender, "invalid-number");
            return;
        }
        var optional = plugin.getKingdomService().getByName(args[1]);
        if (optional.isEmpty()) {
            plugin.getMessageService().send(sender, "kingdom.not-found");
            return;
        }
        plugin.getKingdomService().setLevel(optional.get(), (int) NumberParser.positiveLong(args[2]).getAsLong());
        plugin.getMessageService().send(sender, "admin.level-set", Map.of("kingdom", optional.get().getName()));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            help(sender);
            return;
        }
        KingdomService.Result result = plugin.getKingdomService().deleteByAdmin(args[1], sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : null);
        if (result == KingdomService.Result.SUCCESS) {
            plugin.getMessageService().send(sender, "admin.deleted", Map.of("kingdom", args[1]));
        } else {
            plugin.getKingdomCommand().sendResult(sender, result);
        }
    }

    private void boss(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kingdomcore.boss.start")) {
            plugin.getMessageService().send(sender, "no-permission");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("start")) {
            if (plugin.getBossService().startBoss()) {
                plugin.getMessageService().send(sender, "boss.started", Map.of("boss", plugin.getConfig().getString("boss.name", "Boss")));
            } else {
                plugin.getMessageService().send(sender, "boss.already-active");
            }
            return;
        }
        help(sender);
    }

    private void money(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kingdomcore.economy.admin")) {
            plugin.getMessageService().send(sender, "no-permission");
            return;
        }
        if (args.length < 4 || NumberParser.positiveDouble(args[3]).isEmpty()) {
            plugin.getMessageService().send(sender, "invalid-number");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        double amount = NumberParser.positiveDouble(args[3]).getAsDouble();
        if (args[1].equalsIgnoreCase("give")) {
            plugin.getEconomyService().deposit(target, amount);
            plugin.getMessageService().send(sender, "admin.money-given", Map.of("player", args[2]));
        } else if (args[1].equalsIgnoreCase("take")) {
            plugin.getEconomyService().withdraw(target, amount);
            plugin.getMessageService().send(sender, "admin.money-taken", Map.of("player", args[2]));
        } else {
            help(sender);
        }
    }

    private void help(CommandSender sender) {
        plugin.getMessageService().sendLines(sender, "admin.help", Map.of());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "giveprestige", "removeprestige", "setlevel", "delete", "boss", "money");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("boss")) {
            return List.of("start");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("money")) {
            return List.of("give", "take");
        }
        return List.of();
    }
}
