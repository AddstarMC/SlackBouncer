package au.com.addstar.slackbouncer.commands;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import au.com.addstar.slackapi.Attachment;
import au.com.addstar.slackapi.Attachment.AttachmentField;
import au.com.addstar.slackapi.MessageOptions;
import au.com.addstar.slackapi.MessageOptions.ParseMode;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.cubespace.geSuit.Utilities;
import net.cubespace.geSuit.geSuit;
import net.cubespace.geSuit.managers.ConfigManager;
import net.cubespace.geSuit.managers.DatabaseManager;
import net.cubespace.geSuit.managers.GeoIPManager;
import net.cubespace.geSuit.managers.PlayerManager;
import net.cubespace.geSuit.objects.Ban;
import net.cubespace.geSuit.objects.GSPlayer;
import net.cubespace.geSuit.objects.Track;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class GeSuitCommandHandler implements ISlackCommandHandler
{
	@Override
	public String getUsage( String command )
	{
		switch (command)
		{
		case "seen":
			return "seen <player|uuid>";
		case "where":
			return "where <player|uuid>";
		case "names":
			return "names <player|uuid>";
		case "warnhistory":
			return "warnhistory <player|uuid>";
		}
		
		return null;
	}
	
	@Override
	public void onCommand( SlackCommandSender sender, String command, String[] args ) throws IllegalStateException, IllegalArgumentException
	{
		switch (command)
		{
		case "seen":
			onSeen(sender, args);
			break;
		case "where":
			onWhere(sender, args);
			break;
		case "names":
			onNames(sender, args);
			break;
		case "warnhistory":
			onWarnHistory(sender, args);
			break;
		}
	}
	
	public void onSeen(SlackCommandSender sender, String[] args)
	{
		if (args.length != 1)
			throw new IllegalStateException("seen <player|uuid>");
		
		String seenInfo = ChatColor.translateAlternateColorCodes('&', PlayerManager.getLastSeeninfos(args[0], true, true));
		MessageOptions options = MessageOptions.builder()
			.asUser(true)
			.format(false)
			.mode(ParseMode.None)
			.build();
		sender.sendMessage("\n" + seenInfo, options);
	}
	
	public void onWhere(final SlackCommandSender sender, String[] args)
	{
		if (args.length != 1)
			throw new IllegalStateException("where <player|uuid>");
		
		String search = args[0];
		Attachment attachment = new Attachment("/where results");
		
		List<Track> tracking = null;
		if ( search.contains(".") )
		{
			tracking = DatabaseManager.tracking.getPlayerTracking(search, "ip");
			if ( tracking.isEmpty() )
			{
				sender.sendMessage("No known accounts match or contain \"" + search + "\""); 
				return;
			}
		}
		else
		{
			String type;
			String searchString = search;
			if ( searchString.length() > 20 )
			{
				type = "uuid";
				searchString = searchString.replace("-", "");
			}
			else
				type = "name";

			if ( !DatabaseManager.players.playerExists(searchString) )
			{
				// No exact match... do a partial match

				List<String> matches = DatabaseManager.players.matchPlayers(searchString);
				if ( matches.isEmpty() )
				{
					sender.sendMessage("No known accounts match or contain \"" + searchString + "\"");
					return;
				}
				else if ( matches.size() == 1 )
				{
					if ( searchString.length() < 20 )
						searchString = matches.get(0);
				}
				else
				{
					// Matched too many names, show list of names instead
					attachment.setTitle("Possible Names");
					attachment.setText(Joiner.on('\n').join(matches));
					attachment.setFormatText(false);
					sender.sendMessage("Your query of " + search + " did not match a player", 
						MessageOptions.builder()
							.asUser(true)
							.attachments(Arrays.asList(attachment))
							.mode(ParseMode.None)
							.build()
						);
					return;
				}
			}

			tracking = DatabaseManager.tracking.getPlayerTracking(searchString, type);
			if ( tracking.isEmpty() )
			{
				sender.sendMessage("No known accounts match or contain \"" + searchString + "\"");
				return;
			}
			else
			{
				if ( geSuit.proxy.getPlayer(searchString) != null )
				{
					final ProxiedPlayer player = geSuit.proxy.getPlayer(searchString);
					String location = GeoIPManager.lookup(player.getAddress().getAddress());
					if ( location != null )
						attachment.addField(new AttachmentField("Location", location, false));
				}
			}
		}
		
		attachment.setTitle(String.format("%d accounts associated with %s", tracking.size(), search));
		List<String> lines = Lists.newArrayList();
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		for (Track t : tracking)
		{
			StringBuilder builder = new StringBuilder();
			builder.append(" - ");
			builder.append(t.getPlayer());
			if ( t.isNameBanned() )
			{
				if ( t.getBanType().equals("ban") )
					builder.append("[Ban]");
				else
					builder.append("[Tempban]");
			}
			
			builder.append(' ');
			builder.append(t.getIp());

			if ( t.isIpBanned() )
				builder.append("[IPBan]");
			
			builder.append(" (");
			builder.append(sdf.format(t.getLastSeen()));
			builder.append(')');

			lines.add(builder.toString());
		}
		
		attachment.setText(Joiner.on('\n').join(lines));
		attachment.setFormatText(false);
		sender.sendMessage("Heres the results", 
			MessageOptions.builder()
				.asUser(true)
				.attachments(Arrays.asList(attachment))
				.mode(ParseMode.None)
				.build()
			);
	}
	
	public void onWarnHistory(SlackCommandSender sender, String[] args)
	{
		if ( args.length != 1 )
			throw new IllegalStateException("warnhistory <player|uuid>");

		GSPlayer target = PlayerManager.getPlayer(args[0]);
		String targetId;
		if ( target == null )
		{
			Map<String, UUID> ids = DatabaseManager.players.resolvePlayerNamesHistoric(Arrays.asList(args[0]));
			UUID id = Iterables.getFirst(ids.values(), null);
			if ( id == null )
			{
				sender.sendMessage(args[0] + " has never been warned", MessageOptions.DEFAULT);
				return;
			}
			targetId = id.toString().replace("-", "");
		}
		else
			targetId = target.getUuid();

		List<Ban> warns = DatabaseManager.bans.getWarnHistory(args[0], targetId);
		if ( warns == null || warns.isEmpty() )
		{
			sender.sendMessage(args[0] + " has never been warned", MessageOptions.DEFAULT);
			return;
		}

		Attachment attachment = new Attachment("Warn History");
		attachment.setTitle(args[0] + "'s Warning History");

		List<String> lines = Lists.newArrayList();

		int count = 0;
		for (Ban b : warns)
		{
			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("dd MMM yyyy HH:mm");

			Date now = new Date();
			int age = (int) ((now.getTime() - b.getBannedOn().getTime()) / 1000 / 86400);
			if ( age >= ConfigManager.bans.WarningExpiryDays )
				lines.add(String.format("expired %s - %s by %s", sdf.format(b.getBannedOn()), b.getReason(), b.getBannedBy()));
			else
				lines.add(String.format("%d: %s - %s by %s", ++count, sdf.format(b.getBannedOn()), b.getReason(), b.getBannedBy()));
		}
		
		attachment.setText(Joiner.on('\n').join(lines));
		attachment.setFormatText(false);
		
		sender.sendMessage("Heres the results", 
			MessageOptions.builder()
				.asUser(true)
				.attachments(Arrays.asList(attachment))
				.mode(ParseMode.None)
				.build()
			);
	}
	
	public void onNames(SlackCommandSender sender, String[] args)
	{
		if (args.length != 1)
			throw new IllegalArgumentException("names <player|uuid>");
		
		String nameOrId = args[0];
		UUID id;
		try
		{
			id = Utilities.makeUUID(nameOrId);
		}
		catch ( IllegalArgumentException e )
		{
			Map<String, UUID> result = DatabaseManager.players.resolvePlayerNamesHistoric(Arrays.asList(nameOrId));
			if ( result.isEmpty() )
			{
				sender.sendMessage("Can't find a player by that name", MessageOptions.DEFAULT);
				return;
			}
			else
				id = Iterables.getFirst(result.values(), null);
		}

		List<Track> names = DatabaseManager.tracking.getNameHistory(id);

		Attachment attachment = new Attachment("Name History");
		attachment.setTitle("There are " + names.size() + " names on record for " + nameOrId);
		
		List<String> lines = Lists.newArrayList();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		int index = 0;
		for (Track t : names)
			lines.add(String.format("%d: %s %s", ++index, t.getPlayer(), sdf.format(t.getLastSeen())));
		
		attachment.setText(Joiner.on('\n').join(lines));
		attachment.setFormatText(false);
		
		sender.sendMessage("Heres the results", 
			MessageOptions.builder()
				.asUser(true)
				.attachments(Arrays.asList(attachment))
				.mode(ParseMode.None)
				.build()
			);
	}
}
