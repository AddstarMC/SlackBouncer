package au.com.addstar.slackbouncer.config;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.cubespace.Yamler.Config.Config;

public class MainConfig extends Config
{
	public MainConfig(File file)
	{
		CONFIG_FILE = file;
		channels = Maps.newHashMap();
		commandHandlers = Lists.newArrayList();
	}
	
	public String token = "*unspecified*";
	
	public Map<String, ChannelDefinition> channels;
	public List<String> commandHandlers;
}
