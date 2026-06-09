package pl.kingdomcore.services;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.kingdomcore.KingdomCorePlugin;
import pl.kingdomcore.api.event.KingdomCreateEvent;
import pl.kingdomcore.api.event.KingdomPrestigeChangeEvent;
import pl.kingdomcore.database.KingdomRepository;
import pl.kingdomcore.economy.EconomyService;
import pl.kingdomcore.managers.InviteManager;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.KingdomMember;
import pl.kingdomcore.models.KingdomRole;
import pl.kingdomcore.models.KingdomStats;
import pl.kingdomcore.models.KingdomUpgradeType;
import pl.kingdomcore.models.QuestType;
import pl.kingdomcore.models.RolePermission;
import pl.kingdomcore.models.SerializedLocation;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class KingdomService {
    private final KingdomCorePlugin plugin;
    private final KingdomRepository repository;
    private final EconomyService economyService;
    private final InviteManager inviteManager;
    private final UpgradeService upgradeService;
    private final QuestService questService;
    private final AuditService auditService;
    private final Map<UUID, Kingdom> kingdomsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> idsByLowerName = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> kingdomByPlayer = new ConcurrentHashMap<>();

    public KingdomService(KingdomCorePlugin plugin, KingdomRepository repository, EconomyService economyService,
                          InviteManager inviteManager, UpgradeService upgradeService, QuestService questService,
                          AuditService auditService) {
        this.plugin = plugin;
        this.repository = repository;
        this.economyService = economyService;
        this.inviteManager = inviteManager;
        this.upgradeService = upgradeService;
        this.questService = questService;
        this.auditService = auditService;
    }

    public void load() throws SQLException {
        for (Kingdom kingdom : repository.loadKingdoms()) {
            questService.ensureDefaultQuests(kingdom);
            cache(kingdom);
        }
        plugin.getLogger().info("Loaded " + kingdomsById.size() + " kingdoms.");
    }

    public Collection<Kingdom> getKingdoms() {
        return new ArrayList<>(kingdomsById.values());
    }

    public Optional<Kingdom> getByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        UUID id = idsByLowerName.get(name.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(id == null ? null : kingdomsById.get(id));
    }

    public Optional<Kingdom> getByPlayer(UUID player) {
        UUID kingdomId = kingdomByPlayer.get(player);
        return Optional.ofNullable(kingdomId == null ? null : kingdomsById.get(kingdomId));
    }

    public Optional<Kingdom> getById(UUID id) {
        return Optional.ofNullable(kingdomsById.get(id));
    }

    public boolean hasPermission(Player player, Kingdom kingdom, RolePermission permission) {
        return kingdom.getMember(player.getUniqueId())
                .map(member -> member.getRole().has(permission))
                .orElse(false);
    }

    public Result create(Player player, String name) {
        if (!validName(name)) {
            return Result.INVALID_NAME;
        }
        if (getByPlayer(player.getUniqueId()).isPresent()) {
            return Result.ALREADY_IN_KINGDOM;
        }
        if (getByName(name).isPresent()) {
            return Result.NAME_TAKEN;
        }
        double cost = plugin.getConfig().getDouble("kingdom.create-cost", 500.0);
        if (!economyService.withdraw(player, cost)) {
            return Result.INSUFFICIENT_MONEY;
        }
        UUID id = UUID.randomUUID();
        Map<UUID, KingdomMember> members = new LinkedHashMap<>();
        members.put(player.getUniqueId(), new KingdomMember(
                player.getUniqueId(),
                player.getName(),
                KingdomRole.OWNER,
                Instant.now(),
                0
        ));
        Kingdom kingdom = new Kingdom(
                id,
                name,
                player.getUniqueId(),
                members,
                1,
                0,
                0,
                Instant.now(),
                null,
                new EnumMap<>(KingdomUpgradeType.class),
                new KingdomStats(),
                questService.createDefaultQuests()
        );
        cache(kingdom);
        saveAsync(kingdom);
        Bukkit.getPluginManager().callEvent(new KingdomCreateEvent(player, kingdom));
        auditService.log(id, player.getUniqueId(), "KINGDOM_CREATE", "Created kingdom " + name);
        return Result.SUCCESS;
    }

    public Result disband(Player player) {
        Optional<Kingdom> optional = getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (!kingdom.getOwnerId().equals(player.getUniqueId())) {
            return Result.ROLE_TOO_LOW;
        }
        uncache(kingdom);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.deleteKingdom(kingdom.getId());
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not delete kingdom: " + exception.getMessage());
            }
        });
        auditService.log(kingdom.getId(), player.getUniqueId(), "KINGDOM_DISBAND", kingdom.getName());
        return Result.SUCCESS;
    }

    public Result invite(Player inviter, Player target) {
        Optional<Kingdom> optional = getByPlayer(inviter.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (!hasPermission(inviter, kingdom, RolePermission.INVITE)) {
            return Result.ROLE_TOO_LOW;
        }
        if (getByPlayer(target.getUniqueId()).isPresent()) {
            return Result.ALREADY_IN_KINGDOM;
        }
        if (kingdom.getMembers().size() >= upgradeService.memberLimit(kingdom)) {
            return Result.MEMBER_LIMIT;
        }
        inviteManager.invite(target.getUniqueId(), kingdom.getId());
        auditService.log(kingdom.getId(), inviter.getUniqueId(), "INVITE", target.getName());
        return Result.SUCCESS;
    }

    public Result accept(Player player) {
        Optional<UUID> invite = inviteManager.consume(player.getUniqueId());
        if (invite.isEmpty()) {
            return Result.NO_INVITE;
        }
        Kingdom kingdom = kingdomsById.get(invite.get());
        if (kingdom == null) {
            return Result.NOT_FOUND;
        }
        if (kingdom.getMembers().size() >= upgradeService.memberLimit(kingdom)) {
            return Result.MEMBER_LIMIT;
        }
        kingdom.addMember(new KingdomMember(player.getUniqueId(), player.getName(), KingdomRole.RECRUIT, Instant.now(), 0));
        kingdomByPlayer.put(player.getUniqueId(), kingdom.getId());
        saveAsync(kingdom);
        auditService.log(kingdom.getId(), player.getUniqueId(), "JOIN", player.getName());
        return Result.SUCCESS;
    }

    public Result leave(Player player) {
        Optional<Kingdom> optional = getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (kingdom.getOwnerId().equals(player.getUniqueId())) {
            return Result.OWNER_CANNOT_LEAVE;
        }
        kingdom.removeMember(player.getUniqueId());
        kingdomByPlayer.remove(player.getUniqueId());
        saveAsync(kingdom);
        auditService.log(kingdom.getId(), player.getUniqueId(), "LEAVE", player.getName());
        return Result.SUCCESS;
    }

    public Result kick(Player actor, OfflinePlayer target) {
        Optional<Kingdom> optional = getByPlayer(actor.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (!hasPermission(actor, kingdom, RolePermission.KICK)) {
            return Result.ROLE_TOO_LOW;
        }
        if (!kingdom.hasMember(target.getUniqueId()) || kingdom.getOwnerId().equals(target.getUniqueId())) {
            return Result.NOT_FOUND;
        }
        kingdom.removeMember(target.getUniqueId());
        kingdomByPlayer.remove(target.getUniqueId());
        saveAsync(kingdom);
        auditService.log(kingdom.getId(), actor.getUniqueId(), "KICK", target.getName());
        return Result.SUCCESS;
    }

    public Result setHome(Player player) {
        Optional<Kingdom> optional = getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (!hasPermission(player, kingdom, RolePermission.SET_HOME)) {
            return Result.ROLE_TOO_LOW;
        }
        kingdom.setHome(SerializedLocation.from(player.getLocation()));
        saveAsync(kingdom);
        auditService.log(kingdom.getId(), player.getUniqueId(), "SET_HOME", player.getWorld().getName());
        return Result.SUCCESS;
    }

    public Result deposit(Player player, double amount) {
        Optional<Kingdom> optional = getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (!hasPermission(player, kingdom, RolePermission.BANK_DEPOSIT)) {
            return Result.ROLE_TOO_LOW;
        }
        if (!economyService.withdraw(player, amount)) {
            return Result.INSUFFICIENT_MONEY;
        }
        kingdom.deposit(amount);
        questService.progress(kingdom, player, QuestType.BANK_DEPOSIT, Math.round(amount));
        saveAsync(kingdom);
        auditService.log(kingdom.getId(), player.getUniqueId(), "BANK_DEPOSIT", String.valueOf(amount));
        return Result.SUCCESS;
    }

    public Result withdraw(Player player, double amount) {
        Optional<Kingdom> optional = getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (!hasPermission(player, kingdom, RolePermission.BANK_WITHDRAW)) {
            return Result.ROLE_TOO_LOW;
        }
        if (!kingdom.withdraw(amount)) {
            return Result.INSUFFICIENT_BANK;
        }
        economyService.deposit(player, amount);
        saveAsync(kingdom);
        auditService.log(kingdom.getId(), player.getUniqueId(), "BANK_WITHDRAW", String.valueOf(amount));
        return Result.SUCCESS;
    }

    public Result buyUpgrade(Player player, KingdomUpgradeType type) {
        Optional<Kingdom> optional = getByPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return Result.NOT_IN_KINGDOM;
        }
        Kingdom kingdom = optional.get();
        if (!hasPermission(player, kingdom, RolePermission.BUY_UPGRADES)) {
            return Result.ROLE_TOO_LOW;
        }
        int nextLevel = kingdom.getUpgradeLevel(type) + 1;
        if (nextLevel > upgradeService.maxLevel(type)) {
            return Result.UPGRADE_MAX;
        }
        double moneyCost = upgradeService.moneyCost(type, nextLevel);
        long prestigeCost = upgradeService.prestigeCost(type, nextLevel);
        if (kingdom.getBank() < moneyCost || kingdom.getPrestige() < prestigeCost) {
            return Result.INSUFFICIENT_BANK;
        }
        kingdom.withdraw(moneyCost);
        kingdom.removePrestige(prestigeCost);
        kingdom.setUpgradeLevel(type, nextLevel);
        saveAsync(kingdom);
        auditService.log(kingdom.getId(), player.getUniqueId(), "UPGRADE", type.name() + " -> " + nextLevel);
        return Result.SUCCESS;
    }

    public void addPrestige(Kingdom kingdom, long amount, String reason) {
        long oldPrestige = kingdom.getPrestige();
        kingdom.addPrestige(amount);
        kingdom.getStats().addEarnedPrestige(amount);
        Bukkit.getPluginManager().callEvent(new KingdomPrestigeChangeEvent(kingdom, oldPrestige, kingdom.getPrestige(), reason));
        saveAsync(kingdom);
    }

    public void removePrestige(Kingdom kingdom, long amount, String reason) {
        long oldPrestige = kingdom.getPrestige();
        kingdom.removePrestige(amount);
        Bukkit.getPluginManager().callEvent(new KingdomPrestigeChangeEvent(kingdom, oldPrestige, kingdom.getPrestige(), reason));
        saveAsync(kingdom);
    }

    public void setLevel(Kingdom kingdom, int level) {
        kingdom.setLevel(level);
        saveAsync(kingdom);
    }

    public void saveAllSync() {
        for (Kingdom kingdom : kingdomsById.values()) {
            try {
                repository.saveKingdom(kingdom);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not save kingdom " + kingdom.getName() + ": " + exception.getMessage());
            }
        }
    }

    public void saveAsync(Kingdom kingdom) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.saveKingdom(kingdom);
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not save kingdom " + kingdom.getName() + ": " + exception.getMessage());
            }
        });
    }

    public Result deleteByAdmin(String name, UUID actor) {
        Optional<Kingdom> optional = getByName(name);
        if (optional.isEmpty()) {
            return Result.NOT_FOUND;
        }
        Kingdom kingdom = optional.get();
        uncache(kingdom);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.deleteKingdom(kingdom.getId());
            } catch (SQLException exception) {
                plugin.getLogger().warning("Could not admin-delete kingdom: " + exception.getMessage());
            }
        });
        auditService.log(kingdom.getId(), actor, "ADMIN_DELETE", kingdom.getName());
        return Result.SUCCESS;
    }

    public java.util.List<Kingdom> top(int limit) {
        return getKingdoms().stream()
                .sorted(Comparator.comparingLong(Kingdom::getPrestige).reversed())
                .limit(limit)
                .toList();
    }

    private boolean validName(String name) {
        int min = plugin.getConfig().getInt("kingdom.min-name-length", 3);
        int max = plugin.getConfig().getInt("kingdom.max-name-length", 18);
        String regex = plugin.getConfig().getString("kingdom.name-regex", "^[A-Za-z0-9_]+$");
        return name != null && name.length() >= min && name.length() <= max && Pattern.matches(regex, name);
    }

    private void cache(Kingdom kingdom) {
        kingdomsById.put(kingdom.getId(), kingdom);
        idsByLowerName.put(kingdom.getName().toLowerCase(Locale.ROOT), kingdom.getId());
        for (UUID member : kingdom.getMembers().keySet()) {
            kingdomByPlayer.put(member, kingdom.getId());
        }
    }

    private void uncache(Kingdom kingdom) {
        kingdomsById.remove(kingdom.getId());
        idsByLowerName.remove(kingdom.getName().toLowerCase(Locale.ROOT));
        for (UUID member : kingdom.getMembers().keySet()) {
            kingdomByPlayer.remove(member);
        }
    }

    public enum Result {
        SUCCESS,
        INVALID_NAME,
        ALREADY_IN_KINGDOM,
        NAME_TAKEN,
        INSUFFICIENT_MONEY,
        NOT_IN_KINGDOM,
        ROLE_TOO_LOW,
        MEMBER_LIMIT,
        NO_INVITE,
        NOT_FOUND,
        OWNER_CANNOT_LEAVE,
        INSUFFICIENT_BANK,
        UPGRADE_MAX
    }
}
