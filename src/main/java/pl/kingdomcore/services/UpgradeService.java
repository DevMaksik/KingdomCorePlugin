package pl.kingdomcore.services;

import org.bukkit.configuration.file.FileConfiguration;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.KingdomUpgradeType;

public class UpgradeService {
    private final FileConfiguration config;

    public UpgradeService(FileConfiguration config) {
        this.config = config;
    }

    public int maxLevel(KingdomUpgradeType type) {
        return config.getInt("upgrades." + type.name() + ".max-level", 1);
    }

    public double moneyCost(KingdomUpgradeType type, int nextLevel) {
        return config.getDouble("upgrades." + type.name() + ".money-cost", 1000.0) * nextLevel;
    }

    public long prestigeCost(KingdomUpgradeType type, int nextLevel) {
        return Math.round(config.getLong("upgrades." + type.name() + ".prestige-cost", 50) * nextLevel);
    }

    public int memberLimit(Kingdom kingdom) {
        int base = config.getInt("kingdom.default-member-limit", 12);
        int bonus = config.getInt("upgrades.MEMBER_LIMIT.member-bonus-per-level", 4);
        return base + kingdom.getUpgradeLevel(KingdomUpgradeType.MEMBER_LIMIT) * bonus;
    }

    public double questRewardMultiplier(Kingdom kingdom) {
        return 1.0 + kingdom.getUpgradeLevel(KingdomUpgradeType.QUEST_REWARDS) * 0.10;
    }

    public int homeDelaySeconds(Kingdom kingdom) {
        int base = config.getInt("kingdom.home-teleport-delay-seconds", 5);
        return Math.max(1, base - kingdom.getUpgradeLevel(KingdomUpgradeType.FASTER_HOME));
    }
}
