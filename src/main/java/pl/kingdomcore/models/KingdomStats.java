package pl.kingdomcore.models;

public class KingdomStats {
    private long playerKills;
    private long memberDeaths;
    private long completedQuests;
    private long defeatedBosses;
    private double earnedMoney;
    private long earnedPrestige;

    public long getPlayerKills() {
        return playerKills;
    }

    public void addPlayerKill() {
        playerKills++;
    }

    public long getMemberDeaths() {
        return memberDeaths;
    }

    public void addMemberDeath() {
        memberDeaths++;
    }

    public long getCompletedQuests() {
        return completedQuests;
    }

    public void addCompletedQuest() {
        completedQuests++;
    }

    public long getDefeatedBosses() {
        return defeatedBosses;
    }

    public void addDefeatedBoss() {
        defeatedBosses++;
    }

    public double getEarnedMoney() {
        return earnedMoney;
    }

    public void addEarnedMoney(double amount) {
        earnedMoney += Math.max(0, amount);
    }

    public long getEarnedPrestige() {
        return earnedPrestige;
    }

    public void addEarnedPrestige(long amount) {
        earnedPrestige += Math.max(0, amount);
    }

    public void setPlayerKills(long playerKills) {
        this.playerKills = playerKills;
    }

    public void setMemberDeaths(long memberDeaths) {
        this.memberDeaths = memberDeaths;
    }

    public void setCompletedQuests(long completedQuests) {
        this.completedQuests = completedQuests;
    }

    public void setDefeatedBosses(long defeatedBosses) {
        this.defeatedBosses = defeatedBosses;
    }

    public void setEarnedMoney(double earnedMoney) {
        this.earnedMoney = earnedMoney;
    }

    public void setEarnedPrestige(long earnedPrestige) {
        this.earnedPrestige = earnedPrestige;
    }
}
