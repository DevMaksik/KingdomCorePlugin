package pl.kingdomcore.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseProvider {
    void init() throws SQLException;

    Connection getConnection() throws SQLException;

    void shutdown();
}
