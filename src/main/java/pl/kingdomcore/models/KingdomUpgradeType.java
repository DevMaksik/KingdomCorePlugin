package pl.kingdomcore.models;

import org.bukkit.Material;

public enum KingdomUpgradeType {
    MEMBER_LIMIT("Większy limit członków", Material.PLAYER_HEAD),
    EXP_BOOST("Bonus do expa", Material.EXPERIENCE_BOTTLE),
    DROP_BOOST("Bonus do dropu", Material.DIAMOND_PICKAXE),
    CHEAPER_REPAIRS("Tańsze naprawy", Material.ANVIL),
    QUEST_REWARDS("Większe nagrody z misji", Material.EMERALD),
    FASTER_HOME("Szybszy cooldown teleportacji", Material.ENDER_PEARL),
    BANK_SLOTS("Dodatkowe sloty banku", Material.CHEST);

    private final String displayName;
    private final Material icon;

    KingdomUpgradeType(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }
}
