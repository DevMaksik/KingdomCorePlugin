package pl.kingdomcore.listeners;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.QuestType;

public class PlayerProgressListener implements Listener {
    private final KingdomCorePlugin plugin;

    public PlayerProgressListener(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getKingdomService().getByPlayer(event.getPlayer().getUniqueId()).ifPresent(kingdom -> {
            Material type = event.getBlock().getType();
            if (type == Material.STONE) {
                plugin.getQuestService().progress(kingdom, event.getPlayer(), QuestType.MINE_STONE, 1);
            }
            if (Tag.LOGS.isTagged(type)) {
                plugin.getQuestService().progress(kingdom, event.getPlayer(), QuestType.CHOP_WOOD, 1);
            }
        });
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntityType() == EntityType.PLAYER) {
            return;
        }
        plugin.getKingdomService().getByPlayer(killer.getUniqueId())
                .ifPresent(kingdom -> plugin.getQuestService().progress(kingdom, killer, QuestType.KILL_MOBS, 1));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        plugin.getKingdomService().getByPlayer(victim.getUniqueId()).ifPresent(kingdom -> {
            kingdom.getStats().addMemberDeath();
            plugin.getKingdomService().saveAsync(kingdom);
        });
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }
        plugin.getKingdomService().getByPlayer(killer.getUniqueId()).ifPresent(kingdom -> {
            kingdom.getStats().addPlayerKill();
            plugin.getQuestService().progress(kingdom, killer, QuestType.PVP_KILLS, 1);
        });
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        plugin.getKingdomService().getByPlayer(event.getPlayer().getUniqueId())
                .ifPresent(kingdom -> plugin.getQuestService().progress(kingdom, event.getPlayer(), QuestType.FISH, 1));
    }
}
