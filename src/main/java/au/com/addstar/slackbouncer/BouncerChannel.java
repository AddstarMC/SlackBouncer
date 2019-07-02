package au.com.addstar.slackbouncer;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import au.com.addstar.slackbouncer.commands.ISlackCommandHandler;
import io.github.slackapi4j.MessageOptions;
import io.github.slackapi4j.exceptions.SlackException;
import io.github.slackapi4j.objects.Conversation;
import io.github.slackapi4j.objects.Message;
import io.github.slackapi4j.objects.User;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.plugin.Listener;

import com.google.common.collect.Lists;


import au.com.addstar.slackbouncer.bouncers.ISlackIncomingBouncer;
import au.com.addstar.slackbouncer.bouncers.ISlackOutgoingBouncer;
import au.com.addstar.slackbouncer.config.ChannelDefinition;

public class BouncerChannel
{
	private final String name;
	private final BouncerPlugin plugin;
	private Conversation slackChannel;
	
	private final List<ISlackIncomingBouncer> incoming;
	private final List<ISlackOutgoingBouncer> outgoing;
	
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
	
	public Conversation getSlackChannel()
	{
		return slackChannel;
	}
	
	public void load( ChannelDefinition def) {
        for (ISlackOutgoingBouncer bouncer : outgoing) {
            if (bouncer instanceof Listener)
                plugin.getProxy().getPluginManager().unregisterListener((Listener) bouncer);
        }

        incoming.clear();
        outgoing.clear();

        if (def.incoming != null) {
            for (Entry<String, ConfigSection> entry : def.incoming.entrySet()) {
                ISlackIncomingBouncer bouncer = plugin.makeIncomingBouncer(entry.getKey());
                if (bouncer == null) {
                    plugin.getLogger().warning("Unknown incoming bouncer definition " + entry.getKey() + " in channel " + name);
                    return;
                }
                try {
                    bouncer.load(entry.getValue());
                    incoming.add(bouncer);
                } catch (InvalidConfigurationException e) {
                    plugin.getLogger().severe("Unable to load incoming bouncer " + entry.getKey() + " in channel " + name + ": " + e.getMessage());
                }

            }

            if (def.outgoing != null) {
                for (Entry<String, ConfigSection> entry : def.outgoing.entrySet()) {
                    ISlackOutgoingBouncer bouncer = plugin.makeOutgoingBouncer(entry.getKey(), this);
                    if(bouncer instanceof ISlackCommandHandler){
                      plugin.registerCommandHandler((ISlackCommandHandler)bouncer,"cache_"+name);
                    }
                    if (bouncer == null) {
						plugin.getLogger().warning("Unknown outgoing bouncer definition " + entry.getKey() + " in channel " + name);
						return;
					}
                    try {
                        bouncer.load(entry.getValue());
                        if (bouncer instanceof Listener)
                            plugin.getProxy().getPluginManager().registerListener(plugin, (Listener) bouncer);

                        outgoing.add(bouncer);
                    } catch (InvalidConfigurationException e) {
                        plugin.getLogger().severe("Unable to load outgoing bouncer " + entry.getKey() + " in channel " + name + ": " + e.getMessage());
                    }
                }
            }
        }
    }
	
	void link(Conversation channel)
	{
		slackChannel = channel;
	}
	
	public void onMessage(String message, User sender, Message.MessageType type)
	{
		for (ISlackIncomingBouncer bouncer : incoming)
			bouncer.onMessage(message, sender, type);
	}
	
	public void sendMessage(String message)
	{
		sendMessage(message, MessageOptions.DEFAULT);
	}
	
	public void sendMessage(String message, User target)
	{
		sendMessage(message, target, MessageOptions.DEFAULT);
	}
	
	public void sendMessage(String message, MessageOptions options)
	{
		try
		{
			plugin.getBouncer().getSlack().sendMessage(message, slackChannel, options);
		}
		catch (IOException e)
		{
			plugin.getLogger().severe("An IOException occurred while sending a message:");
			e.printStackTrace();
		}
		catch (SlackException e)
		{
			plugin.getLogger().severe("Slack refused the message with: " + e.getMessage());
		}
		catch (NullPointerException e){
			plugin.getLogger().severe("The slack channel you tried to message was null" + e.getMessage());
		}
	}
	
	public void sendMessage(String message, User target, MessageOptions options)
	{
		try
		{
			plugin.getBouncer().getSlack().sendMessage(String.format("<@%s> %s", target.getId(), message), slackChannel, options);
		}
		catch (IOException e)
		{
			plugin.getLogger().severe("An IOException occurred while sending a message:");
			e.printStackTrace();
		}
		catch (SlackException e)
		{
			plugin.getLogger().severe("Slack refused the message with: " + e.getMessage());
		}
	}

}
