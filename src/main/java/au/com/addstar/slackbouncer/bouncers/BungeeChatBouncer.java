package au.com.addstar.slackbouncer.bouncers;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.event.BCChatEvent;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.slackapi.Message.MessageType;
import au.com.addstar.slackapi.User;
import au.com.addstar.slackbouncer.BouncerChannel;
import au.com.addstar.slackbouncer.SlackUtils;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeChatBouncer implements ISlackIncomingBouncer, ISlackOutgoingBouncer, Listener
{
	private Map<String, ChatChannel> mChannels;
	
	private BouncerChannel mChannel;
	
	public BungeeChatBouncer()
	{
		mChannels = Maps.newHashMap();
	}
	
	public BungeeChatBouncer(BouncerChannel channel)
	{
		mChannels = Maps.newHashMap();
		mChannel = channel;
	}
	
	@Override
	public void load(ConfigSection section) throws InvalidConfigurationException
	{
		mChannels.clear();
		if (section.has("channel"))
		{
			String name = section.get("channel");
			ChatChannel channel = BungeeChat.instance.getChannel(name);
			if (channel != null)
				mChannels.put(name, channel);
		}
		
		if (section.has("channels"))
		{
			List<String> names = section.get("channels");
			for (String name : names)
			{
				ChatChannel channel = BungeeChat.instance.getChannel(name);
				if (channel != null)
					mChannels.put(name, channel);
			}
		}
	}
	
	private String format(String format, String message, User user)
	{
		format = ChatColor.translateAlternateColorCodes('&', format);
		
		format = format.replace("{DISPLAYNAME}", user.getName());
		format = format.replace("{RAWDISPLAYNAME}", user.getName());
		format = format.replace("{NAME}", user.getName());
		format = format.replace("{MESSAGE}", message);

		format = format.replace("{SERVER}", "Slack");
		
		format = format.replace("{GROUP}", "Server");
		format = format.replace("{WORLD}", "");
		
		return format;
	}
	
	private static Pattern mDeTokenPattern = Pattern.compile("\\{(DISPLAYNAME|RAWDISPLAYNAME|NAME|MESSAGE|SERVER|GROUP|WORLD)\\}");
	private String reformatMessage(String format, String srcMessage)
	{
		format = ChatColor.translateAlternateColorCodes('&', format);
		StringBuffer buffer = new StringBuffer();
		int groupNo = 1;
		
		int nameGroup = -1;
		int messageGroup = -1;
		
		// Build an extraction pattern
		int pos = 0;
		Matcher m = mDeTokenPattern.matcher(format);
		while (m.find())
		{
			buffer.append(Pattern.quote(format.substring(pos, m.start())));
			if (m.group(1).equals("DISPLAYNAME") || m.group(1).equals("RAWDISPLAYNAME") || m.group(1).equals("NAME"))
			{
				buffer.append("(.*?)");
				nameGroup = groupNo++;
			}
			else if (m.group(1).equals("MESSAGE"))
			{
				buffer.append("(.*?)");
				messageGroup = groupNo++;
			}
			else
				buffer.append(".*?");
			
			pos = m.end();
		}
		
		buffer.append(Pattern.quote(format.substring(pos)));
		
		Pattern extractPattern = Pattern.compile(buffer.toString());
		
		// Perform the extraction
		m = extractPattern.matcher(srcMessage);
		if (m.matches())
		{
			String username = ChatColor.stripColor(m.group(nameGroup));
			String message = ChatColor.stripColor(m.group(messageGroup));
			
			return String.format("*%s:* %s", username, message);
		}
		else
			return srcMessage;
	}
	
	@Override
	public void onMessage( String message, User sender, MessageType type )
	{
		if (type != MessageType.Normal)
			return;
		
		message = SlackUtils.toMC(message);
		for (Entry<String, ChatChannel> channel : mChannels.entrySet())
		{
			String formatted;
			if (Strings.isNullOrEmpty(channel.getValue().format))
				formatted = message;
			else
				formatted = format(channel.getValue().format, message, sender);
			
			BungeeChat.instance.getPacketManager().broadcast(new MirrorPacket(channel.getKey(), formatted));
			if (!channel.getKey().startsWith("~"))
				ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText(formatted));
		}
	}
	
	@EventHandler
	public void onChatMessage(BCChatEvent event)
	{
		ChatChannel chatChannel = mChannels.get(event.getChannel());
		if (chatChannel != null)
		{
			// Undo the channel formatting
			String finalMessage;
			
			if (Strings.isNullOrEmpty(chatChannel.format))
				finalMessage = event.getMessage();
			else
				finalMessage = reformatMessage(chatChannel.format, event.getMessage());
			
			mChannel.sendMessage(finalMessage);
		}
	}
}
