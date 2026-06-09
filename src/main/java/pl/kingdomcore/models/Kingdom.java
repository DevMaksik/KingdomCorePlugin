package pl.kingdomcore.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Kingdom {
    private final UUID id;
    private final String name;
    private UUID ownerId;
    private final Map<UUID, KingdomMember> members;
    private int level;
    private long prestige;
    private double bank;
    private final Instant createdAt;
    private SerializedLocation home;
    private final Map<KingdomUpgradeType, Integer> upgrades;
    private final KingdomStats stats;
    private final List<KingdomQuest> quests;

    public Kingdom(UUID id, String name, UUID ownerId, Map<UUID, KingdomMember> members, int level, long prestige,
                   double bank, Instant createdAt, SerializedLocation home, Map<KingdomUpgradeType, Integer> upgrades,
                   KingdomStats stats, List<KingdomQuest> quests) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.members = new LinkedHashMap<>(members);
        this.level = level;
        this.prestige = prestige;
        this.bank = bank;
        this.createdAt = createdAt;
        this.home = home;
        this.upgrades = new EnumMap<>(KingdomUpgradeType.class);
        this.upgrades.putAll(upgrades);
        this.stats = stats;
        this.quests = new ArrayList<>(quests);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Map<UUID, KingdomMember> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public Optional<KingdomMember> getMember(UUID uuid) {
        return Optional.ofNullable(members.get(uuid));
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public void addMember(KingdomMember member) {
        members.put(member.getUuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public long getPrestige() {
        return prestige;
    }

    public void addPrestige(long amount) {
        prestige = Math.max(0, prestige + amount);
    }

    public void removePrestige(long amount) {
        prestige = Math.max(0, prestige - Math.max(0, amount));
    }

    public double getBank() {
        return bank;
    }

    public void deposit(double amount) {
        bank += Math.max(0, amount);
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || bank < amount) {
            return false;
        }
        bank -= amount;
        return true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public SerializedLocation getHome() {
        return home;
    }

    public void setHome(SerializedLocation home) {
        this.home = home;
    }

    public int getUpgradeLevel(KingdomUpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeLevel(KingdomUpgradeType type, int level) {
        upgrades.put(type, Math.max(0, level));
    }

    public Map<KingdomUpgradeType, Integer> getUpgrades() {
        return Collections.unmodifiableMap(upgrades);
    }

    public KingdomStats getStats() {
        return stats;
    }

    public List<KingdomQuest> getQuests() {
        return quests;
    }
}
