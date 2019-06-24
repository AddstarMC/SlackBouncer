package au.com.addstar.slackbouncer.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import au.com.addstar.slackapi.objects.Conversation;
import au.com.addstar.slackapi.objects.Message;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import au.com.addstar.slackapi.MessageOptions;
import au.com.addstar.slackapi.objects.User;
import au.com.addstar.slackapi.exceptions.SlackException;
import au.com.addstar.slackbouncer.Bouncer;
import au.com.addstar.slackbouncer.BouncerPlugin;
import au.com.addstar.slackbouncer.SlackUtils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class SlackCommandSender implements CommandSender
{
	private BouncerPlugin plugin;
	private Bouncer bouncer;

	public User getUser() {
		return user;
	}

	private final User user;
	private Conversation channel;
	
	private boolean hasDoneTarget;
	
	private ScheduledTask sendTask;
	private List<String> messages;
	
	public SlackCommandSender(BouncerPlugin plugin, Bouncer bouncer, User user, Conversation channel)
	{
		this.plugin = plugin;
		this.bouncer = bouncer;
		this.user = user;
		this.channel = channel;
		
		messages = Lists.newArrayList();
		hasDoneTarget = false;
	}
	
	@Override
	public void addGroups( String... groups ) {}
	@Override
	public void setPermission( String perm, boolean value ) {}

	@Override
	public Collection<String> getGroups()
	{
		return Collections.emptyList();
	}

	@Override
	public String getName()
	{
		return user.getName();
	}

	@Override
	public Collection<String> getPermissions()
	{
		return Collections.emptyList();
	}

	@Override
	public boolean hasPermission( String perm )
	{
		return true;
	}

	@Override
	public void removeGroups( String... groups ) {}

	@Override
	public void sendMessage( String message )
	{
		synchronized(messages)
		{
			messages.add(message);
			
			startSendDelay();
		}
	}

	@Override
	public void sendMessage( BaseComponent... message )
	{
		sendMessage(TextComponent.toLegacyText(message));
	}

	@Override
	public void sendMessage( BaseComponent message )
	{
		sendMessage(TextComponent.toLegacyText(message));
	}

	@Override
	public void sendMessages( String... message )
	{
		synchronized(messages)
		{
			Collections.addAll(messages, message);
			
			startSendDelay();
		}
	}
	
	private void startSendDelay()
	{
		if (sendTask == null)
		{
			sendTask = plugin.getProxy().getScheduler().schedule(plugin, () -> {
				synchronized(messages)
				{
					if (!messages.isEmpty())
					{
						String combined = Joiner.on('\n').join(messages);
						messages.clear();
						sendMessage(combined, MessageOptions.DEFAULT);
					}
					sendTask = null;
				}
			}, 1, TimeUnit.SECONDS);
		}
	}
	
	public void sendMessage(String message, MessageOptions options)
	{
		if (!hasDoneTarget)
		{
			message = String.format("<@%s> %s", user.getId(), message);
			hasDoneTarget = true;
		}
		
		try
		{
			bouncer.getSlack().sendMessage(SlackUtils.toSlack(message), channel, options);
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
	
	public void sendMessage(String[] message, MessageOptions options)
	{
		if (message.length == 0)
			return;
		
		if (!hasDoneTarget)
		{
			message[0] = String.format("<@%s> %s", user.getId(), message[0]);
			hasDoneTarget = true;
		}
		
		String combined = Joiner.on('\n').join(message);
		sendMessage(combined,options);
	}

	public boolean isSlackAdmin(){
		return user.isAdmin();
	}

	public void sendMessage(Message message){
		try {
			bouncer.getSlack().sendMessage(message);
		} catch (IOException e) {
			plugin.getLogger().severe("An IOException occurred while sending a message:");
			e.printStackTrace();
		} catch (SlackException e) {
			plugin.getLogger().severe("Slack refused the message with: " + e.getMessage());
		}
	}

	public Message createSlackMessage(){
		return Message.builder().
				sourceId(channel.getId())
				.userId(user.getId())
				.as_user(true)
				.blocks(new ArrayList<>())
				.subtype(Message.MessageType.Normal)
				// .thread_ts(response.getTs())
				.build();
	}

}
