package au.com.addstar.slackbouncer;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class BouncerCommand extends Command
{
	private BouncerPlugin plugin;
	public BouncerCommand(BouncerPlugin plugin)
	{
		super("!slack", "slackbouncer.command", "slackbouncer");
		this.plugin = plugin;
	}
	
	@Override
	public void execute( CommandSender sender, String[] args )
	{
		try
		{
			if (args.length == 0)
			{
				sender.sendMessage(TextComponent.fromLegacyText("Expected sub command"));
				return;
			}
			
			switch (args[0].toLowerCase())
			{
			case "reload":
				plugin.reloadConfig();
				sender.sendMessage(TextComponent.fromLegacyText("Bouncer has been reloaded"));
				break;
			}
		}
		catch (IllegalArgumentException e)
		{
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + e.getMessage()));
		}
	}
}
