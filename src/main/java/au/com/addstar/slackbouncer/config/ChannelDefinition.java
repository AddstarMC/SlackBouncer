package au.com.addstar.slackbouncer.config;

import java.util.Map;

import com.google.common.collect.Maps;

import net.cubespace.Yamler.Config.Config;
import net.cubespace.Yamler.Config.ConfigSection;

public class ChannelDefinition extends Config
{
	public ChannelDefinition()
	{
		incoming = Maps.newHashMap();
		outgoing = Maps.newHashMap();
	}
	
	public Map<String, ConfigSection> incoming;
	public Map<String, ConfigSection> outgoing;
	
	@Override
	public String toString()
	{
		return String.format("{Incoming: %s Outgoing: %s}", incoming, outgoing);
	}
}
