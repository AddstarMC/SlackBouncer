package au.com.addstar.slackbouncer.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import au.com.addstar.slackbouncer.BouncerPlugin;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import au.com.addstar.slackapi.objects.Attachment;
import au.com.addstar.slackapi.MessageOptions;
import au.com.addstar.slackapi.objects.Attachment.AttachmentField;
import au.com.addstar.slackapi.MessageOptions.ParseMode;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ProxyCommandHandler implements ISlackCommandHandler
{
	private BouncerPlugin plugin;
	public ProxyCommandHandler(BouncerPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public String getUsage( String command )
	{
		return command;
	}

	@Override
	public void onCommand( SlackCommandSender sender, String command, String[] args ) throws IllegalStateException, IllegalArgumentException
	{
		switch (command.toLowerCase())
		{
		case "who":
		case "list":
			onWho(sender);
			break;
		case "monitor":
			if(args.length < 1) {
			
			}
			String pName = args[0];
			ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
			
		}
	}
	
	public void onWho(SlackCommandSender sender)
	{
		Collection<ProxiedPlayer> players = ProxyServer.getInstance().getPlayers();
		Attachment attachment = new Attachment(players.size() + " players online");
		attachment.setTitle(players.size() + " players online");
		attachment.setFormatFields(false);

		ListMultimap<String, String> groups = ArrayListMultimap.create();
		for (ProxiedPlayer player : players)
		{
			String serverName;
			if (player.getServer() != null)
				serverName = player.getServer().getInfo().getName();
			else
				serverName = "Not Joined";
			
			groups.put(serverName, player.getDisplayName());
		}
		
		List<String> sortedKeys = Lists.newArrayList(groups.keySet());
		Collections.sort(sortedKeys);
		for(String key : sortedKeys)
		{
			List<String> groupPlayers = Lists.newArrayList(groups.get(key));
			Collections.sort(groupPlayers);
			attachment.addField(new AttachmentField(String.format("%s (%d players)", key, groupPlayers.size()), Joiner.on(", ").join(groupPlayers), false));
		}
		
		sender.sendMessage("", 
			MessageOptions.builder()
				.asUser(true)
				.attachments(Arrays.asList(attachment))
				.mode(ParseMode.None)
				.build()
			);
	}

}
