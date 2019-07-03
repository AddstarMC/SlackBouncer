package au.com.addstar.slackbouncer.config;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.YamlConfig;

public class MainConfig extends YamlConfig
{
	public MainConfig(File file)
	{
		CONFIG_FILE = file;
		channels = Maps.newHashMap();
		commandHandlers = Lists.newArrayList();
		ticketConfig = new ConfigSection();
		ticketConfig.set("enabled",false);
		ConfigSection mysql = new ConfigSection();
		mysql.set("hostname","localhost");
        mysql.set("hostport","3306");
        mysql.set("database","databaseName");
        ConfigSection props = new ConfigSection();
        props.set("useSSL",false);
        props.set("user","username");
        props.set("password","password");
        mysql.set("properties",props);
        ticketConfig.set("MYSQL", mysql);
	}
	
	public final String token = "*unspecified*";
	public final Map<String, ChannelDefinition> channels;
	public final List<String> commandHandlers;
	public final ConfigSection ticketConfig;

}
