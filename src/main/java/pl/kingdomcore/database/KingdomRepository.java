package pl.kingdomcore.database;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.kingdomcore.models.Kingdom;
import pl.kingdomcore.models.KingdomMember;
import pl.kingdomcore.models.KingdomQuest;
import pl.kingdomcore.models.KingdomRole;
import pl.kingdomcore.models.KingdomStats;
import pl.kingdomcore.models.KingdomUpgradeType;
import pl.kingdomcore.models.QuestType;
import pl.kingdomcore.models.SerializedLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class KingdomRepository {
    private final DatabaseProvider database;

    public KingdomRepository(DatabaseProvider database) {
        this.database = database;
    }

    public List<Kingdom> loadKingdoms() throws SQLException {
        Map<UUID, Kingdom> kingdoms = new LinkedHashMap<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM kingdoms")) {
            while (resultSet.next()) {
                UUID id = UUID.fromString(resultSet.getString("id"));
                SerializedLocation home = null;
                String world = resultSet.getString("home_world");
                if (world != null) {
                    home = new SerializedLocation(
                            world,
                            resultSet.getDouble("home_x"),
                            resultSet.getDouble("home_y"),
                            resultSet.getDouble("home_z"),
                            (float) resultSet.getDouble("home_yaw"),
                            (float) resultSet.getDouble("home_pitch")
                    );
                }
                Kingdom kingdom = new Kingdom(
                        id,
                        resultSet.getString("name"),
                        UUID.fromString(resultSet.getString("owner")),
                        new LinkedHashMap<>(),
                        resultSet.getInt("level"),
                        resultSet.getLong("prestige"),
                        resultSet.getDouble("bank"),
                        Instant.ofEpochMilli(resultSet.getLong("created_at")),
                        home,
                        deserializeUpgrades(resultSet.getString("upgrades")),
                        deserializeStats(resultSet.getString("stats")),
                        deserializeQuests(resultSet.getString("quests"))
                );
                kingdoms.put(id, kingdom);
            }
        }

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM kingdom_members")) {
            while (resultSet.next()) {
                UUID kingdomId = UUID.fromString(resultSet.getString("kingdom_id"));
                Kingdom kingdom = kingdoms.get(kingdomId);
                if (kingdom != null) {
                    kingdom.addMember(new KingdomMember(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name"),
                            KingdomRole.valueOf(resultSet.getString("role")),
                            Instant.ofEpochMilli(resultSet.getLong("joined_at")),
                            resultSet.getLong("contribution")
                    ));
                }
            }
        }
        return new ArrayList<>(kingdoms.values());
    }

    public void saveKingdom(Kingdom kingdom) throws SQLException {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM kingdoms WHERE id = ?")) {
                    delete.setString(1, kingdom.getId().toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO kingdoms "
                        + "(id, name, owner, level, prestige, bank, created_at, home_world, home_x, home_y, home_z, home_yaw, home_pitch, upgrades, stats, quests) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    insert.setString(1, kingdom.getId().toString());
                    insert.setString(2, kingdom.getName());
                    insert.setString(3, kingdom.getOwnerId().toString());
                    insert.setInt(4, kingdom.getLevel());
                    insert.setLong(5, kingdom.getPrestige());
                    insert.setDouble(6, kingdom.getBank());
                    insert.setLong(7, kingdom.getCreatedAt().toEpochMilli());
                    SerializedLocation home = kingdom.getHome();
                    if (home == null) {
                        insert.setString(8, null);
                        insert.setObject(9, null);
                        insert.setObject(10, null);
                        insert.setObject(11, null);
                        insert.setObject(12, null);
                        insert.setObject(13, null);
                    } else {
                        insert.setString(8, home.world());
                        insert.setDouble(9, home.x());
                        insert.setDouble(10, home.y());
                        insert.setDouble(11, home.z());
                        insert.setDouble(12, home.yaw());
                        insert.setDouble(13, home.pitch());
                    }
                    insert.setString(14, serializeUpgrades(kingdom));
                    insert.setString(15, serializeStats(kingdom.getStats()));
                    insert.setString(16, serializeQuests(kingdom.getQuests()));
                    insert.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM kingdom_members WHERE kingdom_id = ?")) {
                    delete.setString(1, kingdom.getId().toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO kingdom_members "
                        + "(kingdom_id, uuid, name, role, joined_at, contribution) VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (KingdomMember member : kingdom.getMembers().values()) {
                        insert.setString(1, kingdom.getId().toString());
                        insert.setString(2, member.getUuid().toString());
                        insert.setString(3, member.getName());
                        insert.setString(4, member.getRole().name());
                        insert.setLong(5, member.getJoinedAt().toEpochMilli());
                        insert.setLong(6, member.getQuestContribution());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void deleteKingdom(UUID kingdomId) throws SQLException {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement members = connection.prepareStatement("DELETE FROM kingdom_members WHERE kingdom_id = ?");
                 PreparedStatement kingdoms = connection.prepareStatement("DELETE FROM kingdoms WHERE id = ?")) {
                members.setString(1, kingdomId.toString());
                members.executeUpdate();
                kingdoms.setString(1, kingdomId.toString());
                kingdoms.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public double getBalance(UUID uuid, double startingBalance) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT balance FROM player_balances WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                }
            }
        }
        setBalance(uuid, startingBalance);
        return startingBalance;
    }

    public void setBalance(UUID uuid, double balance) throws SQLException {
        try (Connection connection = database.getConnection()) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM player_balances WHERE uuid = ?")) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO player_balances (uuid, balance) VALUES (?, ?)")) {
                insert.setString(1, uuid.toString());
                insert.setDouble(2, Math.max(0, balance));
                insert.executeUpdate();
            }
        }
    }

    public void appendAudit(UUID kingdomId, UUID actor, String action, String details) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO audit_logs "
                     + "(kingdom_id, actor, action, details, created_at) VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, kingdomId == null ? null : kingdomId.toString());
            statement.setString(2, actor == null ? null : actor.toString());
            statement.setString(3, action);
            statement.setString(4, details);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private String serializeUpgrades(Kingdom kingdom) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<KingdomUpgradeType, Integer> entry : kingdom.getUpgrades().entrySet()) {
            yaml.set(entry.getKey().name(), entry.getValue());
        }
        return yaml.saveToString();
    }

    private Map<KingdomUpgradeType, Integer> deserializeUpgrades(String raw) {
        Map<KingdomUpgradeType, Integer> upgrades = new EnumMap<>(KingdomUpgradeType.class);
        YamlConfiguration yaml = loadYaml(raw);
        for (String key : yaml.getKeys(false)) {
            try {
                upgrades.put(KingdomUpgradeType.valueOf(key), yaml.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // Unknown upgrade from a future version is ignored for forward compatibility.
            }
        }
        return upgrades;
    }

    private String serializeStats(KingdomStats stats) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("player-kills", stats.getPlayerKills());
        yaml.set("member-deaths", stats.getMemberDeaths());
        yaml.set("completed-quests", stats.getCompletedQuests());
        yaml.set("defeated-bosses", stats.getDefeatedBosses());
        yaml.set("earned-money", stats.getEarnedMoney());
        yaml.set("earned-prestige", stats.getEarnedPrestige());
        return yaml.saveToString();
    }

    private KingdomStats deserializeStats(String raw) {
        YamlConfiguration yaml = loadYaml(raw);
        KingdomStats stats = new KingdomStats();
        stats.setPlayerKills(yaml.getLong("player-kills"));
        stats.setMemberDeaths(yaml.getLong("member-deaths"));
        stats.setCompletedQuests(yaml.getLong("completed-quests"));
        stats.setDefeatedBosses(yaml.getLong("defeated-bosses"));
        stats.setEarnedMoney(yaml.getDouble("earned-money"));
        stats.setEarnedPrestige(yaml.getLong("earned-prestige"));
        return stats;
    }

    private String serializeQuests(List<KingdomQuest> quests) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (KingdomQuest quest : quests) {
            String path = "quests." + quest.getId() + ".";
            yaml.set(path + "name", quest.getName());
            yaml.set(path + "description", quest.getDescription());
            yaml.set(path + "type", quest.getType().name());
            yaml.set(path + "target", quest.getTarget());
            yaml.set(path + "progress", quest.getProgress());
            yaml.set(path + "money-reward", quest.getMoneyReward());
            yaml.set(path + "prestige-reward", quest.getPrestigeReward());
            yaml.set(path + "weekly", quest.isWeekly());
            yaml.set(path + "completed", quest.isCompleted());
            yaml.set(path + "reset-at", quest.getResetAt().toEpochMilli());
        }
        return yaml.saveToString();
    }

    private List<KingdomQuest> deserializeQuests(String raw) {
        YamlConfiguration yaml = loadYaml(raw);
        List<KingdomQuest> quests = new ArrayList<>();
        if (!yaml.isConfigurationSection("quests")) {
            return quests;
        }
        for (String id : yaml.getConfigurationSection("quests").getKeys(false)) {
            String path = "quests." + id + ".";
            Optional<QuestType> type = parseQuestType(yaml.getString(path + "type"));
            if (type.isEmpty()) {
                continue;
            }
            quests.add(new KingdomQuest(
                    id,
                    yaml.getString(path + "name", id),
                    yaml.getString(path + "description", ""),
                    type.get(),
                    yaml.getLong(path + "target"),
                    yaml.getLong(path + "progress"),
                    yaml.getDouble(path + "money-reward"),
                    yaml.getLong(path + "prestige-reward"),
                    yaml.getBoolean(path + "weekly"),
                    yaml.getBoolean(path + "completed"),
                    Instant.ofEpochMilli(yaml.getLong(path + "reset-at", System.currentTimeMillis()))
            ));
        }
        return quests;
    }

    private Optional<QuestType> parseQuestType(String raw) {
        try {
            return Optional.of(QuestType.valueOf(raw));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Optional.empty();
        }
    }

    private YamlConfiguration loadYaml(String raw) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (raw == null || raw.isBlank()) {
            return yaml;
        }
        try {
            yaml.loadFromString(raw);
        } catch (InvalidConfigurationException ignored) {
            return new YamlConfiguration();
        }
        return yaml;
    }
}
