package pl.kingdomcore.services;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.KingdomQuest;
import pl.kingdomcore.models.QuestType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestService {
    private final KingdomCorePlugin plugin;
    private final UpgradeService upgradeService;

    public QuestService(KingdomCorePlugin plugin, UpgradeService upgradeService) {
        this.plugin = plugin;
        this.upgradeService = upgradeService;
    }

    public List<KingdomQuest> createDefaultQuests() {
        List<KingdomQuest> quests = new ArrayList<>();
        loadSection(quests, "quests.daily", false, Instant.now().plus(1, ChronoUnit.DAYS));
        loadSection(quests, "quests.weekly", true, Instant.now().plus(7, ChronoUnit.DAYS));
        return quests;
    }

    public void ensureDefaultQuests(Kingdom kingdom) {
        if (kingdom.getQuests().isEmpty()) {
            kingdom.getQuests().addAll(createDefaultQuests());
        }
    }

    public void progress(Kingdom kingdom, Player contributor, QuestType type, long amount) {
        boolean changed = false;
        for (KingdomQuest quest : kingdom.getQuests()) {
            if (quest.getType() == type && !quest.isCompleted()) {
                quest.addProgress(amount);
                changed = true;
                if (quest.isCompleted()) {
                    reward(kingdom, quest);
                }
            }
        }
        kingdom.getMember(contributor.getUniqueId()).ifPresent(member -> member.addQuestContribution(amount));
        if (changed) {
            plugin.getKingdomService().saveAsync(kingdom);
        }
    }

    public void resetExpired(Kingdom kingdom) {
        Instant now = Instant.now();
        for (KingdomQuest quest : kingdom.getQuests()) {
            if (quest.getResetAt().isBefore(now)) {
                quest.reset(now.plus(quest.isWeekly() ? 7 : 1, ChronoUnit.DAYS));
            }
        }
    }

    private void reward(Kingdom kingdom, KingdomQuest quest) {
        double money = quest.getMoneyReward() * upgradeService.questRewardMultiplier(kingdom);
        kingdom.deposit(money);
        plugin.getKingdomService().addPrestige(kingdom, quest.getPrestigeReward(), "quest:" + quest.getId());
        kingdom.getStats().addCompletedQuest();
        kingdom.getStats().addEarnedMoney(money);
    }

    private void loadSection(List<KingdomQuest> quests, String path, boolean weekly, Instant resetAt) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            String questPath = path + "." + id + ".";
            Optional<QuestType> type = parseType(plugin.getConfig().getString(questPath + "type"));
            if (type.isEmpty()) {
                continue;
            }
            quests.add(new KingdomQuest(
                    id,
                    plugin.getConfig().getString(questPath + "name", id),
                    plugin.getConfig().getString(questPath + "description", ""),
                    type.get(),
                    plugin.getConfig().getLong(questPath + "target", 1),
                    0,
                    plugin.getConfig().getDouble(questPath + "money-reward", 0),
                    plugin.getConfig().getLong(questPath + "prestige-reward", 0),
                    weekly,
                    false,
                    resetAt
            ));
        }
    }

    private Optional<QuestType> parseType(String raw) {
        try {
            return Optional.of(QuestType.valueOf(raw));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Optional.empty();
        }
    }
}
