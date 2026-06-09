package pl.kingdomcore.services;

import org.bukkit.Bukkit;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.database.KingdomRepository;

import java.sql.SQLException;
import java.util.UUID;

public class AuditService {
    private final KingdomCorePlugin plugin;
    private final KingdomRepository repository;

    public AuditService(KingdomCorePlugin plugin, KingdomRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void log(UUID kingdomId, UUID actor, String action, String details) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.appendAudit(kingdomId, actor, action, details);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not write audit log: " + exception.getMessage());
            }
        });
    }
}
