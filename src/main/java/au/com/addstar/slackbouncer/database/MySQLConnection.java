package au.com.addstar.slackbouncer.database;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 25/06/2019.
 */
public class MySQLConnection {
    private static Logger log = Logger.getLogger("SlackBouncer");
    private String hostname;
    private String port;
    private String database;
    private Properties properties;
    private Connection connection;

    public MySQLConnection(String host,String port, String database, Properties properties) throws SQLException {
        this.hostname = "localhost";
        this.port = "3306";
        this.database = "simpletickets";
        this.hostname = host;
        this.port = port;
        this.properties = properties;
        this.database = database;
        try {
            this.open();
        }catch (SQLException e){
            log.warning("MYSQL Database will not be available.");
        }
    }

    public Connection getConnection() throws SQLException {

            if (connection == null) {
                this.open();
                return connection;
            }else
                if(connection.isClosed())
                    this.open();
                return connection;
    }

    public void open() throws SQLException{
        String url = "";
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.warning("");
        }
        url = "jdbc:mysql://" + this.hostname + ":" + this.port + "/" + this.database;
        this.connection = DriverManager.getConnection(url, this.properties);
    }



    public void close() {
        if(connection == null)
            return;
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
