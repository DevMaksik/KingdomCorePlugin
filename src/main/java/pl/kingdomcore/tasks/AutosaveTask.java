package pl.kingdomcore.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import pl.kingdomcore.KingdomCorePlugin;

public class AutosaveTask extends BukkitRunnable {
    private final KingdomCorePlugin plugin;

    public AutosaveTask(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getKingdomService().saveAllSync();
    }
}
