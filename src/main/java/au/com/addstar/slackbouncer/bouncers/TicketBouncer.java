package au.com.addstar.slackbouncer.bouncers;

import me.odium.simplehelptickets.database.Database;
import me.odium.simplehelptickets.database.MySQLConnection;
import me.odium.simplehelptickets.manager.TicketManager;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.plugin.Listener;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 16/05/2019.
 */
public class TicketBouncer implements ISlackOutgoingBouncer, Listener {
    private TicketManager manager;
    @Override
    public void load(ConfigSection section) throws InvalidConfigurationException {
        ConfigSection mysql = section.get("MySQL");
        if(mysql == null) throw new InvalidConfigurationException("No MYSQL config found");
        String host = section.get("hostname");
        String port = section.get("hostport");
        String dbName = section.get("database");
        Properties properties = new Properties();
        ConfigSection props = mysql.get("properties");
        properties.put("useSSL", props.get("useSSL").toString());
        Logger log = Logger.getLogger("SlackBouncer");
        Database database = new MySQLConnection(host,port,properties,dbName,log);
        manager = new TicketManager(database,log,false);
    }

}

