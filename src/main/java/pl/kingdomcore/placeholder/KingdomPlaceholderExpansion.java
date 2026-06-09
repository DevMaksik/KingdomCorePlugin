package pl.kingdomcore.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.models.Kingdom;

import java.util.Comparator;
import java.util.Optional;

public class KingdomPlaceholderExpansion extends PlaceholderExpansion {
    private final KingdomCorePlugin plugin;

    public KingdomPlaceholderExpansion(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "kingdomcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Portfolio Project";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        Optional<Kingdom> optional = player == null ? Optional.empty() : plugin.getKingdomService().getByPlayer(player.getUniqueId());
        if (params.equalsIgnoreCase("player_balance") && player != null) {
            return String.format("%.2f", plugin.getEconomyService().getBalance(player));
        }
        if (optional.isEmpty()) {
            return "";
        }
        Kingdom kingdom = optional.get();
        return switch (params.toLowerCase()) {
            case "kingdom_name" -> kingdom.getName();
            case "kingdom_level" -> String.valueOf(kingdom.getLevel());
            case "kingdom_prestige" -> String.valueOf(kingdom.getPrestige());
            case "kingdom_rank" -> String.valueOf(rank(kingdom));
            case "player_role" -> kingdom.getMember(player.getUniqueId()).map(member -> member.getRole().name()).orElse("");
            default -> null;
        };
    }

    private int rank(Kingdom kingdom) {
        var sorted = plugin.getKingdomService().getKingdoms().stream()
                .sorted(Comparator.comparingLong(Kingdom::getPrestige).reversed())
                .toList();
        return sorted.indexOf(kingdom) + 1;
    }
}
