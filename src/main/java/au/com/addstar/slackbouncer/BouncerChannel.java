package au.com.addstar.slackbouncer;

import java.util.List;
import java.util.Map.Entry;

import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.plugin.Listener;

import com.google.common.collect.Lists;

import au.com.addstar.slackapi.Channel;
import au.com.addstar.slackbouncer.bouncers.ISlackIncomingBouncer;
import au.com.addstar.slackbouncer.bouncers.ISlackOutgoingBouncer;
import au.com.addstar.slackbouncer.config.ChannelDefinition;

public class BouncerChannel
{
	private final String name;
	private final BouncerPlugin plugin;
	private Channel slackChannel;
	
	private List<ISlackIncomingBouncer> incoming;
	private List<ISlackOutgoingBouncer> outgoing;
	
	public BouncerChannel(String name, BouncerPlugin plugin)
	{
		if (name.startsWith("#"))
			this.name = name.substring(1);
		else
			this.name = name;
		
		this.plugin = plugin;
		
		incoming = Lists.newArrayList();
		outgoing = Lists.newArrayList();
	}
	
	public String getName()
	{
		return name;
	}
	
	public BouncerPlugin getPlugin()
	{
		return plugin;
	}
	
	public void load(ChannelDefinition def)
	{
		incoming.clear();
		outgoing.clear();
		
		if (def.incoming != null)
		{
			for (Entry<String, ConfigSection> entry : def.incoming.entrySet())
			{
				ISlackIncomingBouncer bouncer = plugin.makeIncomingBouncer(entry.getKey());
				if (bouncer == null)
					plugin.getLogger().warning("Unknown incoming bouncer definition " + entry.getKey() + " in channel " + name);
				
				try
				{
					bouncer.load(entry.getValue());
					incoming.add(bouncer);
				}
				catch (InvalidConfigurationException e)
				{
					plugin.getLogger().severe("Unable to load incoming bouncer " + entry.getKey() + " in channel " + name + ": " + e.getMessage());
				}
			}
		}
		
		if (def.outgoing != null)
		{
			for (Entry<String, ConfigSection> entry : def.outgoing.entrySet())
			{
				ISlackOutgoingBouncer bouncer = plugin.makeOutgoingBouncer(entry.getKey(), this);
				if (bouncer == null)
					plugin.getLogger().warning("Unknown outgoing bouncer definition " + entry.getKey() + " in channel " + name);
				
				try
				{
					bouncer.load(entry.getValue());
					if (bouncer instanceof Listener)
						plugin.getProxy().getPluginManager().registerListener(plugin, (Listener)bouncer);
					
					outgoing.add(bouncer);
				}
				catch (InvalidConfigurationException e)
				{
					plugin.getLogger().severe("Unable to load outgoing bouncer " + entry.getKey() + " in channel " + name + ": " + e.getMessage());
				}
			}
		}
	}
	
	void link(Channel channel)
	{
		slackChannel = channel;
	}
}
