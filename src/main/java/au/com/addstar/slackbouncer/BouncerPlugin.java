package au.com.addstar.slackbouncer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import au.com.addstar.slackbouncer.commands.*;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.plugin.Plugin;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import au.com.addstar.slackapi.Attachment;
import au.com.addstar.slackapi.BaseChannel;
import au.com.addstar.slackapi.DirectChannel;
import au.com.addstar.slackapi.GroupChannel;
import au.com.addstar.slackapi.MessageOptions;
import au.com.addstar.slackapi.NormalChannel;
import au.com.addstar.slackapi.User;
import au.com.addstar.slackapi.Message.MessageType;
import au.com.addstar.slackapi.MessageOptions.ParseMode;
import au.com.addstar.slackapi.events.MessageEvent;
import au.com.addstar.slackapi.exceptions.SlackException;
import au.com.addstar.slackbouncer.bouncers.BungeeChatBouncer;
import au.com.addstar.slackbouncer.bouncers.GeSuitBouncer;
import au.com.addstar.slackbouncer.bouncers.ISlackIncomingBouncer;
import au.com.addstar.slackbouncer.bouncers.ISlackOutgoingBouncer;
import au.com.addstar.slackbouncer.config.ChannelDefinition;
import au.com.addstar.slackbouncer.config.MainConfig;

public class BouncerPlugin extends Plugin
{
	private Map<String, Constructor<? extends ISlackIncomingBouncer>> incomingRegistrations;
	private Map<String, Constructor<? extends ISlackOutgoingBouncer>> outgoingRegistrations;
	private Map<String, ISlackCommandHandler> commandHandlers;
	
	private MainConfig config;
	private Bouncer bouncer;
	
	private List<BouncerChannel> channels;
	
	public BouncerPlugin()
	{
		incomingRegistrations = Maps.newHashMap();
		outgoingRegistrations = Maps.newHashMap();
		commandHandlers = Maps.newHashMap();
		channels = Lists.newArrayList();
	}
	
	@Override
	public void onEnable()
	{
		config = new MainConfig(new File(getDataFolder(), "config.yml"));
		
		if (getProxy().getPluginManager().getPlugin("BungeeChat") != null)
		{
			registerIncomingBouncer("bungeechat", BungeeChatBouncer.class);
			registerOutgoingBouncer("bungeechat", BungeeChatBouncer.class);
		}
		
		if (getProxy().getPluginManager().getPlugin("geSuit") != null)
		{
			registerOutgoingBouncer("gesuit", GeSuitBouncer.class);
			registerCommandHandler(new GeSuitCommandHandler(), "seen", "where", "names", "warnhistory", "banhistory", "geo");
			registerCommandHandler(new AdminCommandHandler(), "restart");
		}
		
		registerCommandHandler(new ProxyCommandHandler(), "who", "list");
		
		if (!loadConfig())
			return;
		
		getProxy().getPluginManager().registerCommand(this, new BouncerCommand(this));
		
		if (!tryStartBouncer())
			return;
	}
	
	@Override
	public void onDisable()
	{
		if (bouncer != null)
		{
			bouncer.shutdown();
			bouncer = null;
		}
	}
	
	private boolean loadConfig()
	{
		try
		{
			config.init();
			
			loadChannels();
			return true;
		}
		catch (InvalidConfigurationException e)
		{
			getLogger().severe("Unable to load configuration: " + e.getMessage());
			return false;
		}
	}
	
	private boolean tryStartBouncer()
	{
		if (Strings.isNullOrEmpty(config.token) || config.token.equals("*unspecified*"))
		{
			getLogger().severe("Token is not configured. Please edit the config and set the token");
			return false;
		}
		
		bouncer = new Bouncer(this);
		return true;
	}
	
	private void loadChannels()
	{
		channels.clear();
		for (Entry<String, ChannelDefinition> entry : config.channels.entrySet())
		{
			BouncerChannel channel = new BouncerChannel(entry.getKey(), this);
			channel.load(entry.getValue());
			channels.add(channel);
		}
	}
	
	public boolean reloadConfig()
	{
		if (bouncer != null)
		{
			bouncer.shutdown();
			bouncer = null;
		}
		
		if (!loadConfig())
			return false;
		
		if (!tryStartBouncer())
			return false;
		
		return true;
	}
	
	public void registerIncomingBouncer(String name, Class<? extends ISlackIncomingBouncer> bouncerClass)
	{
		try
		{
			Constructor<? extends ISlackIncomingBouncer> constructor = bouncerClass.getConstructor();
			incomingRegistrations.put(name.toLowerCase(), constructor);
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalArgumentException(bouncerClass.getName() + " does not have a public default constructor");
		}
	}
	
	public void registerOutgoingBouncer(String name, Class<? extends ISlackOutgoingBouncer> bouncerClass)
	{
		try
		{
			Constructor<? extends ISlackOutgoingBouncer> constructor = bouncerClass.getConstructor(BouncerChannel.class);
			outgoingRegistrations.put(name.toLowerCase(), constructor);
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalArgumentException(bouncerClass.getName() + " does not have a public constructor that takes a BouncerChannel");
		}
	}
	
	public void registerCommandHandler(ISlackCommandHandler handler, String... commands)
	{
		for (String command : commands)
			commandHandlers.put(command.toLowerCase(), handler);
	}
	
	public MainConfig getConfig()
	{
		return config;
	}
	
	ISlackIncomingBouncer makeIncomingBouncer(String name)
	{
		Constructor<? extends ISlackIncomingBouncer> constructor = incomingRegistrations.get(name.toLowerCase());
		if (constructor == null)
			return null;
		
		try
		{
			return constructor.newInstance();
		}
		catch (IllegalArgumentException e)
		{
			// Should never happen
			throw new AssertionError(e);
		}
		catch (IllegalAccessException e)
		{
			// Should never happen
			throw new AssertionError(e);
		}
		catch (InstantiationException e)
		{
			throw new RuntimeException(e);
		}
		catch ( InvocationTargetException e )
		{
			throw new RuntimeException(e);
		}
	}
	
	ISlackOutgoingBouncer makeOutgoingBouncer(String name, BouncerChannel channel)
	{
		Constructor<? extends ISlackOutgoingBouncer> constructor = outgoingRegistrations.get(name.toLowerCase());
		if (constructor == null)
			return null;
		
		try
		{
			return constructor.newInstance(channel);
		}
		catch (IllegalArgumentException e)
		{
			// Should never happen
			throw new AssertionError(e);
		}
		catch (IllegalAccessException e)
		{
			// Should never happen
			throw new AssertionError(e);
		}
		catch (InstantiationException e)
		{
			throw new RuntimeException(e);
		}
		catch ( InvocationTargetException e )
		{
			throw new RuntimeException(e);
		}
	}
	
	Bouncer getBouncer()
	{
		return bouncer;
	}
	
	void onLoginComplete()
	{
		for (BouncerChannel bChannel : channels)
		{
			NormalChannel slackChannel = bouncer.getSession().getChannel(bChannel.getName());
			
			if (slackChannel == null)
			{
				getLogger().warning("Unable to join non-existant channel " + bChannel.getName());
				continue;
			}
			
			// Check that the client is in the channel (join if not)
			if (!slackChannel.isClientMember())
			{
				if (slackChannel instanceof GroupChannel)
					getLogger().severe("Not a member of group " + bChannel.getName() + ". Must be invited into group");
				else
				{
					// Try to join the channel
					try
					{
						getLogger().info("Not a member of channel " + bChannel.getName() + ". Joining...");
						bouncer.getSlack().getChannelManager().joinChannel(slackChannel);
						// Now a member, link the real channel to our channel
						bChannel.link(slackChannel);
					}
					catch (IOException e)
					{
						getLogger().severe("An IOException occured while joining channel " + slackChannel.getName());
						e.printStackTrace();
					}
					catch (SlackException e)
					{
						getLogger().severe("Unable to join channel " + slackChannel.getName() + e.toString());
					}
				}
			}
			else
				// Is already a member, link the channel
				bChannel.link(slackChannel);
		}
	}
	
	void onMessage(MessageEvent event)
	{
		// DEBUGGING: Trying to find out what was null in that same user check
		if (event.getUser() == null)
		{
			getLogger().info("[DEBUG] Event user was null");
			return;
		}
		
		if (bouncer == null)
		{
			getLogger().info("[DEBUG] bouncer was null");
			return;
		}
		
		if (bouncer.getSession() == null)
		{
			getLogger().info("[DEBUG] session was null");
			return;
		}
		
		if (event.getUser().equals(bouncer.getSession().getSelf()))
			return;
		
		if (event.getMessage().getText() == null)
			return;
		
		String message = SlackUtils.resolveGroups(event.getMessage().getText(), bouncer.getSession());
		BaseChannel source = bouncer.getSession().getChannelById(event.getMessage().getSourceId());
		
		// Command handling
		if (event.getType() == MessageType.Normal)
		{
			String user = bouncer.getSession().getSelf().getName().toLowerCase();
			
			if (source instanceof DirectChannel || (message.toLowerCase().startsWith(user) || message.toLowerCase().startsWith("@" + user)))
			{
				processCommands(event.getUser(), source, message);
				return;
			}
		}
		
		// Process bouncers
		for (BouncerChannel channel : channels)
		{
			if (channel.getSlackChannel() == null)
				continue;
			
			if (channel.getSlackChannel().getId().equals(event.getMessage().getSourceId()))
				channel.onMessage(message, event.getUser(), event.getType());
		}
	}
	
	private void processCommands(User source, BaseChannel channel, String text)
	{
		int start = 0;
		
		SlackCommandSender sender = new SlackCommandSender(this, bouncer, source, channel);
		
		String[] arguments = text.split(" ");
		
		if (arguments.length == 0)
			return;
		
		String user = bouncer.getSession().getSelf().getName().toLowerCase();
		
		if (arguments[0].toLowerCase().startsWith(user) || arguments[0].toLowerCase().startsWith("@" + user))
			start = 1;
		else
			start = 0;
		
		if (arguments.length < start+1)
		{
			sender.sendMessage("You did not give me a command");
			return;
		}
		
		String command = arguments[start];
		
		if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("commands"))
		{
			Attachment attachment = new Attachment("List of commands");
			attachment.setTitle("The following commands are available");
			
			List<String> commands = Lists.newArrayList(commandHandlers.keySet());
			Collections.sort(commands);
			
			for (int i = 0; i < commands.size(); ++i)
			{
				String cmd = commands.get(i);
				ISlackCommandHandler handler = commandHandlers.get(cmd);
				if (handler != null)
					cmd = handler.getUsage(cmd);
				
				if (cmd != null)
					commands.set(i, cmd);
			}
			
			attachment.setText(Joiner.on('\n').join(commands));
			attachment.setFormatText(false);
			
			sender.sendMessage("", 
				MessageOptions.builder()
					.asUser(true)
					.attachments(Arrays.asList(attachment))
					.mode(ParseMode.None)
					.build()
				);
			
			return;
		}
		
		ISlackCommandHandler handler = commandHandlers.get(command.toLowerCase());
		if (handler == null)
		{
			sender.sendMessage("I dont know what to do with _" + command + "_");
			return;
		}
		
		arguments = Arrays.copyOfRange(arguments, start+1, arguments.length);
		
		try
		{
			handler.onCommand(sender, command, arguments);
		}
		catch (IllegalArgumentException e)
		{
			sender.sendMessage(e.getMessage());
		}
		catch (IllegalStateException e)
		{
			sender.sendMessage("Usage: " + e.getMessage());
		}
	}
}
