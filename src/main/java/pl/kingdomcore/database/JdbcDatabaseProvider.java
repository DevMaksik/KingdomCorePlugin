package pl.kingdomcore.database;

import org.bukkit.configuration.file.FileConfiguration;
import pl.kingdomcore.KingdomCorePlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcDatabaseProvider implements DatabaseProvider {
    private final KingdomCorePlugin plugin;
    private final boolean mysql;
    private String jdbcUrl;
    private String username;
    private String password;

    public JdbcDatabaseProvider(KingdomCorePlugin plugin) {
        this.plugin = plugin;
        this.mysql = plugin.getConfig().getString("database.type", "sqlite").equalsIgnoreCase("mysql");
    }

    @Override
    public void init() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        if (mysql) {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "kingdomcore");
            boolean ssl = config.getBoolean("database.mysql.use-ssl", false);
            username = config.getString("database.mysql.username", "root");
            password = config.getString("database.mysql.password", "");
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        } else {
            File databaseFile = new File(plugin.getDataFolder(), "kingdomcore.db");
            jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        }

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            String textType = mysql ? "LONGTEXT" : "TEXT";
            String doubleType = mysql ? "DOUBLE" : "REAL";
            String auditId = mysql ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS kingdoms ("
                    + "id VARCHAR(36) PRIMARY KEY,"
                    + "name VARCHAR(32) UNIQUE NOT NULL,"
                    + "owner VARCHAR(36) NOT NULL,"
                    + "level INTEGER NOT NULL,"
                    + "prestige BIGINT NOT NULL,"
                    + "bank " + doubleType + " NOT NULL,"
                    + "created_at BIGINT NOT NULL,"
                    + "home_world VARCHAR(64),"
                    + "home_x " + doubleType + ","
                    + "home_y " + doubleType + ","
                    + "home_z " + doubleType + ","
                    + "home_yaw " + doubleType + ","
                    + "home_pitch " + doubleType + ","
                    + "upgrades " + textType + ","
                    + "stats " + textType + ","
                    + "quests " + textType
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS kingdom_members ("
                    + "kingdom_id VARCHAR(36) NOT NULL,"
                    + "uuid VARCHAR(36) NOT NULL,"
                    + "name VARCHAR(32) NOT NULL,"
                    + "role VARCHAR(16) NOT NULL,"
                    + "joined_at BIGINT NOT NULL,"
                    + "contribution BIGINT NOT NULL,"
                    + "PRIMARY KEY (kingdom_id, uuid)"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_balances ("
                    + "uuid VARCHAR(36) PRIMARY KEY,"
                    + "balance " + doubleType + " NOT NULL"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs ("
                    + "id " + auditId + ","
                    + "kingdom_id VARCHAR(36),"
                    + "actor VARCHAR(36),"
                    + "action VARCHAR(64) NOT NULL,"
                    + "details " + textType + ","
                    + "created_at BIGINT NOT NULL"
                    + ")");
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (mysql) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public void shutdown() {
        // DriverManager-backed connections are short lived and closed by repositories.
    }
}
