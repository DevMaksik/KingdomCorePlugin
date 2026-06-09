package pl.kingdomcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.KingdomUpgradeType;
import pl.kingdomcore.models.RolePermission;
import pl.kingdomcore.services.KingdomService;
import pl.kingdomcore.utils.NumberParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KingdomCommand implements CommandExecutor, TabCompleter {
    private final KingdomCorePlugin plugin;

    public KingdomCommand(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "only-player");
            return true;
        }
        if (!player.hasPermission("kingdomcore.use")) {
            plugin.getMessageService().send(player, "no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getMessageService().sendLines(player, "kingdom.help", Map.of());
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> create(player, args);
            case "disband" -> {
                KingdomService.Result result = plugin.getKingdomService().disband(player);
                if (result == KingdomService.Result.SUCCESS) {
                    plugin.getMessageService().send(player, "kingdom.disbanded");
                } else {
                    sendResult(player, result);
                }
            }
            case "invite" -> invite(player, args);
            case "accept" -> accept(player);
            case "kick" -> kick(player, args);
            case "leave" -> {
                KingdomService.Result result = plugin.getKingdomService().leave(player);
                if (result == KingdomService.Result.SUCCESS) {
                    plugin.getMessageService().send(player, "kingdom.left");
                } else {
                    sendResult(player, result);
                }
            }
            case "info" -> info(player, args);
            case "list" -> list(player);
            case "top" -> top(player);
            case "sethome" -> {
                KingdomService.Result result = plugin.getKingdomService().setHome(player);
                if (result == KingdomService.Result.SUCCESS) {
                    plugin.getMessageService().send(player, "kingdom.home-set");
                } else {
                    sendResult(player, result);
                }
            }
            case "home" -> home(player);
            case "bank" -> bank(player);
            case "deposit" -> deposit(player, args);
            case "withdraw" -> withdraw(player, args);
            case "upgrade" -> upgrade(player, args);
            case "quests" -> quests(player);
            case "menu" -> plugin.getKingdomMenu().openMain(player);
            default -> plugin.getMessageService().sendLines(player, "kingdom.help", Map.of());
        }
        return true;
    }

    public void sendResult(CommandSender sender, KingdomService.Result result) {
        switch (result) {
            case SUCCESS -> {
            }
            case INVALID_NAME -> plugin.getMessageService().send(sender, "kingdom.invalid-name");
            case ALREADY_IN_KINGDOM -> plugin.getMessageService().send(sender, "kingdom.already-in-kingdom");
            case NAME_TAKEN -> plugin.getMessageService().send(sender, "kingdom.name-taken");
            case INSUFFICIENT_MONEY -> plugin.getMessageService().send(sender, "kingdom.insufficient-money");
            case NOT_IN_KINGDOM -> plugin.getMessageService().send(sender, "kingdom.not-in-kingdom");
            case ROLE_TOO_LOW -> plugin.getMessageService().send(sender, "kingdom.role-too-low");
            case MEMBER_LIMIT -> plugin.getMessageService().send(sender, "kingdom.member-limit");
            case NO_INVITE -> plugin.getMessageService().send(sender, "kingdom.no-invite");
            case NOT_FOUND -> plugin.getMessageService().send(sender, "kingdom.not-found");
            case OWNER_CANNOT_LEAVE -> plugin.getMessageService().send(sender, "kingdom.owner-cannot-leave");
            case INSUFFICIENT_BANK -> plugin.getMessageService().send(sender, "kingdom.insufficient-bank");
            case UPGRADE_MAX -> plugin.getMessageService().send(sender, "kingdom.upgrade-max");
        }
    }

    private void create(Player player, String[] args) {
        if (!player.hasPermission("kingdomcore.create")) {
            plugin.getMessageService().send(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.getMessageService().sendLines(player, "kingdom.help", Map.of());
            return;
        }
        KingdomService.Result result = plugin.getKingdomService().create(player, args[1]);
        if (result == KingdomService.Result.SUCCESS) {
            plugin.getMessageService().send(player, "kingdom.created", Map.of("kingdom", args[1]));
        } else if (result == KingdomService.Result.INSUFFICIENT_MONEY) {
            plugin.getMessageService().send(player, "kingdom.create-cost", Map.of("amount", String.valueOf(plugin.getConfig().getDouble("kingdom.create-cost"))));
        } else {
            sendResult(player, result);
        }
    }

    private void invite(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendLines(player, "kingdom.help", Map.of());
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessageService().send(player, "player-not-found");
            return;
        }
        KingdomService.Result result = plugin.getKingdomService().invite(player, target);
        if (result == KingdomService.Result.SUCCESS) {
            plugin.getMessageService().send(player, "kingdom.invited", Map.of("player", target.getName()));
            plugin.getKingdomService().getByPlayer(player.getUniqueId()).ifPresent(kingdom ->
                    plugin.getMessageService().send(target, "kingdom.invite-received", Map.of("kingdom", kingdom.getName())));
        } else {
            sendResult(player, result);
        }
    }

    private void accept(Player player) {
        KingdomService.Result result = plugin.getKingdomService().accept(player);
        if (result == KingdomService.Result.SUCCESS) {
            plugin.getKingdomService().getByPlayer(player.getUniqueId()).ifPresent(kingdom ->
                    plugin.getMessageService().send(player, "kingdom.joined", Map.of("kingdom", kingdom.getName())));
        } else {
            sendResult(player, result);
        }
    }

    private void kick(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendLines(player, "kingdom.help", Map.of());
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        KingdomService.Result result = plugin.getKingdomService().kick(player, target);
        if (result == KingdomService.Result.SUCCESS) {
            plugin.getMessageService().send(player, "kingdom.kicked", Map.of("player", args[1]));
        } else {
            sendResult(player, result);
        }
    }

    private void info(Player player, String[] args) {
        var optional = args.length >= 2 ? plugin.getKingdomService().getByName(args[1]) : plugin.getKingdomService().getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            plugin.getMessageService().send(player, "kingdom.not-found");
            return;
        }
        Kingdom kingdom = optional.get();
        plugin.getMessageService().sendLines(player, "kingdom.info", Map.of(
                "name", kingdom.getName(),
                "level", String.valueOf(kingdom.getLevel()),
                "prestige", String.valueOf(kingdom.getPrestige()),
                "bank", String.format(Locale.US, "%.2f", kingdom.getBank()),
                "members", String.valueOf(kingdom.getMembers().size())
        ));
    }

    private void list(Player player) {
        plugin.getMessageService().send(player, "kingdom.list-header");
        plugin.getKingdomService().getKingdoms().stream()
                .limit(10)
                .forEach(kingdom -> player.sendMessage(pl.kingdomcore.utils.Text.color(plugin.getMessageService().raw("kingdom.list-entry", Map.of(
                        "kingdom", kingdom.getName(),
                        "members", String.valueOf(kingdom.getMembers().size())
                )))));
    }

    private void top(Player player) {
        plugin.getMessageService().send(player, "kingdom.top-header");
        int position = 1;
        for (Kingdom kingdom : plugin.getKingdomService().top(10)) {
            player.sendMessage(pl.kingdomcore.utils.Text.color(plugin.getMessageService().raw("kingdom.top-entry", Map.of(
                    "position", String.valueOf(position++),
                    "kingdom", kingdom.getName(),
                    "prestige", String.valueOf(kingdom.getPrestige())
            ))));
        }
    }

    private void home(Player player) {
        var optional = plugin.getKingdomService().getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            plugin.getMessageService().send(player, "kingdom.not-in-kingdom");
            return;
        }
        Kingdom kingdom = optional.get();
        if (!plugin.getKingdomService().hasPermission(player, kingdom, RolePermission.USE_HOME)) {
            plugin.getMessageService().send(player, "kingdom.role-too-low");
            return;
        }
        if (kingdom.getHome() == null || kingdom.getHome().toBukkit() == null) {
            plugin.getMessageService().send(player, "kingdom.no-home");
            return;
        }
        int delay = plugin.getUpgradeService().homeDelaySeconds(kingdom);
        plugin.getMessageService().send(player, "kingdom.teleporting", Map.of("seconds", String.valueOf(delay)));
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.teleport(kingdom.getHome().toBukkit()), delay * 20L);
    }

    private void bank(Player player) {
        var optional = plugin.getKingdomService().getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            plugin.getMessageService().send(player, "kingdom.not-in-kingdom");
            return;
        }
        plugin.getMessageService().send(player, "kingdom.bank", Map.of("amount", String.format(Locale.US, "%.2f", optional.get().getBank())));
    }

    private void deposit(Player player, String[] args) {
        if (args.length < 2 || NumberParser.positiveDouble(args[1]).isEmpty()) {
            plugin.getMessageService().send(player, "invalid-number");
            return;
        }
        double amount = NumberParser.positiveDouble(args[1]).getAsDouble();
        KingdomService.Result result = plugin.getKingdomService().deposit(player, amount);
        if (result == KingdomService.Result.SUCCESS) {
            plugin.getMessageService().send(player, "kingdom.deposited", Map.of("amount", String.valueOf(amount)));
        } else {
            sendResult(player, result);
        }
    }

    private void withdraw(Player player, String[] args) {
        if (args.length < 2 || NumberParser.positiveDouble(args[1]).isEmpty()) {
            plugin.getMessageService().send(player, "invalid-number");
            return;
        }
        double amount = NumberParser.positiveDouble(args[1]).getAsDouble();
        KingdomService.Result result = plugin.getKingdomService().withdraw(player, amount);
        if (result == KingdomService.Result.SUCCESS) {
            plugin.getMessageService().send(player, "kingdom.withdrawn", Map.of("amount", String.valueOf(amount)));
        } else {
            sendResult(player, result);
        }
    }

    private void upgrade(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getKingdomMenu().openMain(player);
            return;
        }
        try {
            KingdomUpgradeType type = KingdomUpgradeType.valueOf(args[1].toUpperCase(Locale.ROOT));
            KingdomService.Result result = plugin.getKingdomService().buyUpgrade(player, type);
            if (result == KingdomService.Result.SUCCESS) {
                Kingdom kingdom = plugin.getKingdomService().getByPlayer(player.getUniqueId()).orElseThrow();
                plugin.getMessageService().send(player, "kingdom.upgrade-bought", Map.of("upgrade", type.getDisplayName(), "level", String.valueOf(kingdom.getUpgradeLevel(type))));
            } else {
                sendResult(player, result);
            }
        } catch (IllegalArgumentException exception) {
            player.sendMessage(pl.kingdomcore.utils.Text.color(plugin.getMessageService().raw("kingdom.upgrades-available", Map.of("upgrades", String.join(", ", upgrades())))));
        }
    }

    private void quests(Player player) {
        var optional = plugin.getKingdomService().getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            plugin.getMessageService().send(player, "kingdom.not-in-kingdom");
            return;
        }
        optional.get().getQuests().forEach(quest ->
                player.sendMessage(pl.kingdomcore.utils.Text.color(plugin.getMessageService().raw("kingdom.quest-entry", Map.of(
                        "quest", quest.getName(),
                        "progress", String.valueOf(quest.getProgress()),
                        "target", String.valueOf(quest.getTarget())
                )))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "disband", "invite", "accept", "kick", "leave", "info", "list", "top", "home", "sethome", "upgrade", "bank", "deposit", "withdraw", "quests", "menu");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("upgrade")) {
            return upgrades();
        }
        return new ArrayList<>();
    }

    private List<String> upgrades() {
        return java.util.Arrays.stream(KingdomUpgradeType.values()).map(Enum::name).map(String::toLowerCase).toList();
    }
}
