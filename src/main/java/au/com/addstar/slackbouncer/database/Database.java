package au.com.addstar.slackbouncer.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 2/07/2019.
 */
public interface Database {
    void open() throws SQLException;
    Connection getConnection() throws SQLException;
    void close();
}
