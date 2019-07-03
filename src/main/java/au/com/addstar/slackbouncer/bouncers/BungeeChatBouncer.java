package au.com.addstar.slackbouncer.bouncers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
 import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.addstar.slackbouncer.commands.ISlackCommandHandler;
import au.com.addstar.slackbouncer.commands.SlackCommandSender;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.event.BCChatEvent;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.slackbouncer.BouncerChannel;
import au.com.addstar.slackbouncer.SlackUtils;
import com.google.common.collect.Queues;
import io.github.slackapi4j.MessageOptions;
import io.github.slackapi4j.objects.Message;
import io.github.slackapi4j.objects.User;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeChatBouncer implements ISlackIncomingBouncer, ISlackOutgoingBouncer, Listener, ISlackCommandHandler
{
	private static final Pattern mDeTokenPattern = Pattern.compile("\\{(DISPLAYNAME|RAWDISPLAYNAME|NAME|MESSAGE|SERVER|GROUP|WORLD)\\}");
	private LinkedBlockingQueue<String> messageCache;
	private Boolean cached;
	private Integer cacheSize =20;
	private BouncerChannel mChannel;
	
	public BungeeChatBouncer()
	{
		mChannels = Maps.newHashMap();
		messageCache = Queues.newLinkedBlockingQueue(cacheSize);
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
		cached = section.has("cached") && section.<Boolean>get("cached");
		if(cached){
			cacheSize = (section.has("cacheSize"))?section.<Integer>get("cachedSize"):20;
            messageCache = Queues.newLinkedBlockingQueue(cacheSize);
        }else{
			cacheSize = 0;
			messageCache = null;
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
	private final Map<String, ChatChannel> mChannels;
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
	public void onMessage( String message, User sender, Message.MessageType type )
	{
		if (type != Message.MessageType.Normal)
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
			if(!cached){
			    mChannel.sendMessage(finalMessage);
			}else{
			    messageCache.offer(finalMessage);
            }
		}
	}

    @Override
    public String getUsage(String command) {
        return "check_<channel_name>";
    }

    @Override
    public void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException {
	    List<String> out = new ArrayList<>();
	    int size = messageCache.size();
	    out.add("Last " + size + " Messages on this monitor");
        for(String message: messageCache){
            out.add(message);
            messageCache.remove(message);
        }
        String[] mess= new String[out.size()];
        out.toArray(mess);
        sender.sendMessage(mess, MessageOptions.DEFAULT);
    }
}
