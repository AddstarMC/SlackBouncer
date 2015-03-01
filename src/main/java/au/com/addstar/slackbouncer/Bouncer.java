package au.com.addstar.slackbouncer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import au.com.addstar.slackapi.RealTimeListener;
import au.com.addstar.slackapi.RealTimeSession;
import au.com.addstar.slackapi.SlackAPI;
import au.com.addstar.slackapi.events.MessageEvent;
import au.com.addstar.slackapi.events.RealTimeEvent;
import au.com.addstar.slackapi.exceptions.SlackException;
import au.com.addstar.slackapi.exceptions.SlackRTException;

public class Bouncer implements RealTimeListener
{
	private BouncerPlugin plugin;
	private SlackAPI slack;
	private RealTimeSession session;
	private boolean isClosing;
	
	public Bouncer(BouncerPlugin plugin)
	{
		this.plugin = plugin;
		
		slack = new SlackAPI(plugin.getConfig().token);
		connect();
	}
	
	public SlackAPI getSlack()
	{
		return slack;
	}
	
	public RealTimeSession getSession()
	{
		return session;
	}
	
	public void shutdown()
	{
		isClosing = true;
		if (session != null)
		{
			session.close();
			session = null;
		}
	}
	
	private void connect()
	{
		isClosing = false;
		ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					session = slack.startRTSession();
					session.addListener(Bouncer.this);
				}
				catch ( SlackException e )
				{
					plugin.getLogger().severe("Unable to connect to Slack service: " + e.toString());
				}
				catch ( IOException e )
				{
					plugin.getLogger().severe("Unable to connect to Slack service:");
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void onLoginComplete()
	{
		plugin.getLogger().info("Logged into Slack as " + session.getSelf().getName());
		plugin.onLoginComplete();
	}

	@Override
	public void onEvent( RealTimeEvent event )
	{
		if (event instanceof MessageEvent)
			plugin.onMessage((MessageEvent)event);
	}

	@Override
	public void onError( SlackRTException exception )
	{
		plugin.getLogger().warning("An error occured: " + exception.toString());
	}

	@Override
	public void onClose()
	{
		session = null;
		if (!isClosing)
		{
			ProxyServer.getInstance().getScheduler().schedule(plugin, new Runnable()
			{
				@Override
				public void run()
				{
					if (!isClosing)
						connect();
				}
			}, 5, TimeUnit.SECONDS);
		}
	}
	
}
