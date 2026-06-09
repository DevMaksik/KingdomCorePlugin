package pl.kingdomcore.models;

import java.time.Instant;

public class KingdomQuest {
    private final String id;
    private final String name;
    private final String description;
    private final QuestType type;
    private final long target;
    private long progress;
    private final double moneyReward;
    private final long prestigeReward;
    private final boolean weekly;
    private boolean completed;
    private Instant resetAt;

    public KingdomQuest(String id, String name, String description, QuestType type, long target, long progress,
                        double moneyReward, long prestigeReward, boolean weekly, boolean completed, Instant resetAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.target = target;
        this.progress = Math.max(0, progress);
        this.moneyReward = moneyReward;
        this.prestigeReward = prestigeReward;
        this.weekly = weekly;
        this.completed = completed;
        this.resetAt = resetAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public long getTarget() {
        return target;
    }

    public long getProgress() {
        return progress;
    }

    public void addProgress(long amount) {
        if (!completed) {
            progress = Math.min(target, progress + Math.max(0, amount));
            completed = progress >= target;
        }
    }

    public double getMoneyReward() {
        return moneyReward;
    }

    public long getPrestigeReward() {
        return prestigeReward;
    }

    public boolean isWeekly() {
        return weekly;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getResetAt() {
        return resetAt;
    }

    public void reset(Instant resetAt) {
        this.progress = 0;
        this.completed = false;
        this.resetAt = resetAt;
    }
}
