package pl.kingdomcore.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import pl.kingdomcore.KingdomCorePlugin;

public class QuestResetTask extends BukkitRunnable {
    private final KingdomCorePlugin plugin;

    public QuestResetTask(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getKingdomService().getKingdoms().forEach(kingdom -> {
            plugin.getQuestService().resetExpired(kingdom);
            plugin.getKingdomService().saveAsync(kingdom);
        });
    }
}
