package au.com.addstar.slackbouncer.bouncers;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;

import au.com.addstar.slackbouncer.BouncerChannel;
import io.github.slackapi4j.MessageOptions;
import io.github.slackapi4j.objects.Attachment;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.geSuit.events.BanPlayerEvent;
import net.cubespace.geSuit.events.UnbanPlayerEvent;
import net.cubespace.geSuit.events.WarnPlayerEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class GeSuitBouncer implements ISlackOutgoingBouncer, Listener
{


	private BouncerChannel mChannel;
	
	private boolean showBans;
	private boolean showUnbans;
	private boolean showWarns;
	
	public GeSuitBouncer(BouncerChannel channel)
	{
		mChannel = channel;
	}
	
	@Override
	public void load( ConfigSection section )
	{
		showBans = section.has("bans") && section.<Boolean>get("bans");
		showUnbans = section.has("unbans") && section.<Boolean>get("unbans");
		showWarns = section.has("warns") && section.<Boolean>get("warns");
	}
	
	@EventHandler
	public void onBan(BanPlayerEvent event)
	{
		if (!showBans || event.isAutomatic())
			return;
		
		Attachment attachment = new Attachment("");
		attachment.addField(new Attachment.AttachmentField("Reason", event.getReason(), false));
		switch (event.getType())
		{
		default:
		case Name:
			attachment.setFallback(String.format("BanNotice: %s was banned by %s", event.getPlayerName(), event.getBannedBy()));
			attachment.setTitle("Ban");
			attachment.setText(String.format("%s has been banned by %s.", event.getPlayerName(), event.getBannedBy()));
			break;
		case IP:
			attachment.setFallback(String.format("BanNotice: %s (%s) was IP banned by %s", event.getPlayerName(), event.getPlayerIP().getHostAddress(), event.getBannedBy()));
			attachment.setTitle("IP Ban");
			attachment.setText(String.format("%s has been ip banned by %s.", event.getPlayerName(), event.getBannedBy()));
			attachment.addField(new Attachment.AttachmentField("IP", event.getPlayerIP().getHostAddress(), true));
			break;
		case Temporary:
			attachment.setFallback(String.format("BanNotice: %s was temp banned by %s until %s", event.getPlayerName(), event.getBannedBy(), DateFormat.getDateTimeInstance().format(event.getUnbanDate())));
			attachment.setTitle("Temp Ban");
			attachment.setText(String.format("%s has been temp banned by %s.", event.getPlayerName(), event.getBannedBy()));
			attachment.addField(new Attachment.AttachmentField("Until", DateFormat.getDateTimeInstance().format(event.getUnbanDate()), true));
			break;
		}
		
		attachment.setColor("danger");
		attachment.setFormatText(false);
		attachment.setFormatFields(true);
		
		MessageOptions options = MessageOptions.builder()
			.username("Ban Bot")
			.asUser(false)
			.attachments(Collections.singletonList(attachment))
			.mode(MessageOptions.ParseMode.Full)
			.build();
		
		mChannel.sendMessage("", options);
	}
	
	@EventHandler
	public void onWarn(WarnPlayerEvent event)
	{
		if (!showWarns)
			return;
		
		Attachment attachment = new Attachment(String.format("WarnNotice: %s was warned by %s for %s", event.getPlayerName(), event.getBy(), event.getReason()));
		attachment.setTitle("Warn");
		attachment.setColor("warning");
		attachment.setText(String.format("%s has been warned by %s.", event.getPlayerName(), event.getBy()));
		attachment.addField(new Attachment.AttachmentField("Reason", event.getReason(), false));
		if (event.getActionExtra().isEmpty())
			attachment.addField(new Attachment.AttachmentField("Action", String.format("#%d - %s", event.getWarnCount(), event.getAction()), true));
		else
			attachment.addField(new Attachment.AttachmentField("Action", String.format("#%d - %s %s", event.getWarnCount(), event.getAction(), event.getActionExtra()), true));
		
		attachment.setFormatText(false);
		attachment.setFormatFields(true);
		
		MessageOptions options = MessageOptions.builder()
			.username("Warn Bot")
			.asUser(false)
			.attachments(Collections.singletonList(attachment))
			.mode(MessageOptions.ParseMode.Full)
			.build();
		
		mChannel.sendMessage("", options);
	}
	
	@EventHandler
	public void onUnban(UnbanPlayerEvent event)
	{
		if (!showUnbans)
			return;
		
		Attachment attachment = new Attachment("");
		attachment.setFallback(String.format("UnbanNotice: %s was unbanned by %s", event.getPlayerName(), event.getBannedBy()));
		attachment.setTitle("Unban");
		attachment.setText(String.format("%s has been unbanned by %s", event.getPlayerName(), event.getUnbannedBy()));

		attachment.setColor("good");
		attachment.setFormatText(false);
		attachment.setFormatFields(true);
		
		MessageOptions options = MessageOptions.builder()
			.username("Ban Bot")
			.asUser(false)
			.attachments(Collections.singletonList(attachment))
			.mode(MessageOptions.ParseMode.Full)
			.build();
		
		mChannel.sendMessage("", options);
	}
}
