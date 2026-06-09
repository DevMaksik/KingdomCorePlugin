package pl.kingdomcore.services;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.QuestType;
import pl.kingdomcore.utils.Text;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class BossService implements Listener {
    private final KingdomCorePlugin plugin;
    private final Map<UUID, Double> damage = new HashMap<>();
    private LivingEntity activeBoss;
    private BossBar bossBar;
    private int taskId = -1;

    public BossService(KingdomCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startBoss() {
        if (activeBoss != null && !activeBoss.isDead()) {
            return false;
        }
        World world = Bukkit.getWorld(plugin.getConfig().getString("boss.world", "world"));
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        Location location = new Location(
                world,
                plugin.getConfig().getDouble("boss.x"),
                plugin.getConfig().getDouble("boss.y"),
                plugin.getConfig().getDouble("boss.z")
        );
        EntityType type = EntityType.valueOf(plugin.getConfig().getString("boss.entity-type", "WITHER"));
        activeBoss = (LivingEntity) world.spawnEntity(location, type);
        double health = plugin.getConfig().getDouble("boss.max-health", 600.0);
        if (activeBoss.getAttribute(Attribute.MAX_HEALTH) != null) {
            activeBoss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        }
        activeBoss.setHealth(health);
        activeBoss.customName(Text.color(plugin.getConfig().getString("boss.name", "&5Pradawny Monarcha")));
        activeBoss.setCustomNameVisible(true);
        damage.clear();

        bossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("boss.name", "&5Pradawny Monarcha")),
                BarColor.PURPLE,
                BarStyle.SEGMENTED_12
        );
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
        plugin.getMessageService().send(Bukkit.getConsoleSender(), "boss.started", Map.of("boss", activeBoss.getName()));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessageService().raw("prefix")
                + plugin.getMessageService().raw("boss.started", Map.of("boss", activeBoss.getName()))));
        startAttackTask();
        return true;
    }

    public Map<UUID, Double> topDamage() {
        return damage.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(plugin.getConfig().getInt("boss.top-damage-entries", 5))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (activeBoss == null || !event.getEntity().getUniqueId().equals(activeBoss.getUniqueId())) {
            return;
        }
        Player player = damager(event);
        if (player != null) {
            damage.merge(player.getUniqueId(), event.getFinalDamage(), Double::sum);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (activeBoss == null || !event.getEntity().getUniqueId().equals(activeBoss.getUniqueId())) {
            return;
        }
        rewardTopDamage();
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessageService().raw("prefix") + plugin.getMessageService().raw("boss.defeated")));
        cleanup();
    }

    private void rewardTopDamage() {
        double money = plugin.getConfig().getDouble("boss.reward-money", 2500.0);
        long prestige = plugin.getConfig().getLong("boss.reward-prestige", 100);
        topDamage().entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry<UUID, Double>::getValue).reversed())
                .forEach(entry -> {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null) {
                        return;
                    }
                    plugin.getEconomyService().deposit(player, money);
                    Optional<Kingdom> kingdom = plugin.getKingdomService().getByPlayer(player.getUniqueId());
                    kingdom.ifPresent(value -> {
                        value.getStats().addDefeatedBoss();
                        plugin.getKingdomService().addPrestige(value, prestige, "boss");
                        plugin.getQuestService().progress(value, player, QuestType.BOSS_KILLS, 1);
                    });
                });
    }

    private void startAttackTask() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeBoss == null || activeBoss.isDead()) {
                cleanup();
                return;
            }
            double progress = Math.max(0.0, activeBoss.getHealth() / maxHealth());
            bossBar.setProgress(Math.min(1.0, progress));
            for (Player player : activeBoss.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(activeBoss.getLocation()) > 35 * 35) {
                    continue;
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                Vector pull = activeBoss.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.45);
                player.setVelocity(player.getVelocity().add(pull));
            }
            activeBoss.getWorld().createExplosion(activeBoss.getLocation(), 0.0F, false, false);
            activeBoss.getWorld().spawnParticle(org.bukkit.Particle.FLAME, activeBoss.getLocation().add(0, 1.5, 0), 40, 2, 1, 2, 0.02);
            if (activeBoss.getHealth() < maxHealth() * 0.45) {
                activeBoss.getWorld().dropItemNaturally(activeBoss.getLocation(), new org.bukkit.inventory.ItemStack(Material.BLAZE_ROD));
            }
        }, 20L, 200L).getTaskId();
    }

    private double maxHealth() {
        if (activeBoss == null || activeBoss.getAttribute(Attribute.MAX_HEALTH) == null) {
            return plugin.getConfig().getDouble("boss.max-health", 600.0);
        }
        return activeBoss.getAttribute(Attribute.MAX_HEALTH).getValue();
    }

    private Player damager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private void cleanup() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        activeBoss = null;
    }
}
