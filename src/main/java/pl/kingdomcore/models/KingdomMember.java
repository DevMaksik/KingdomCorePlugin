package pl.kingdomcore.models;

import java.time.Instant;
import java.util.UUID;

public class KingdomMember {
    private final UUID uuid;
    private String name;
    private KingdomRole role;
    private final Instant joinedAt;
    private long questContribution;

    public KingdomMember(UUID uuid, String name, KingdomRole role, Instant joinedAt, long questContribution) {
        this.uuid = uuid;
        this.name = name;
        this.role = role;
        this.joinedAt = joinedAt;
        this.questContribution = questContribution;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KingdomRole getRole() {
        return role;
    }

    public void setRole(KingdomRole role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public long getQuestContribution() {
        return questContribution;
    }

    public void addQuestContribution(long amount) {
        this.questContribution += Math.max(0, amount);
    }
}
