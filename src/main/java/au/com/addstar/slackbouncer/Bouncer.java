package au.com.addstar.slackbouncer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
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
	private boolean isConnecting;
	private ScheduledTask checkTask;
	
	private final Object lockObject = new Object();
	
	public Bouncer(BouncerPlugin plugin)
	{
		this.plugin = plugin;
		
		slack = new SlackAPI(plugin.getConfig().token);
		connect();
		//startReconnectionTask();
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
		synchronized(lockObject)
		{
			isClosing = true;
			if (session != null)
			{
				session.close();
				session = null;
			}
			
			if (checkTask != null)
			{
				checkTask.cancel();
				checkTask = null;
			}
		}
	}
	
	private void startReconnectionTask()
	{
		checkTask = plugin.getProxy().getScheduler().schedule(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				if (!isConnecting && session == null)
					connect();
			}
		}, 10, 10, TimeUnit.SECONDS);
	}
	
	private void connect()
	{
		synchronized(lockObject)
		{
			if (session != null || isConnecting)
				return;
			
			isClosing = false;
			isConnecting = true;
		}
		
		ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				synchronized(lockObject)
				{
					isConnecting = false;
					try
					{
						session = slack.startRTSession();
						session.addListener(Bouncer.this);
					}
					catch ( SlackException e )
					{
						plugin.getLogger().severe("Unable to connect to Slack service: " + e.toString());
						session = null;
					}
					catch (SocketTimeoutException e)
					{
						plugin.getLogger().severe("Unable to connect to Slack service: Connection Timeout");
						session = null;
						
						// Retry in a few seconds
						ProxyServer.getInstance().getScheduler().schedule(plugin, new Runnable()
						{
							@Override
							public void run()
							{
								synchronized(lockObject)
								{
									if (!isClosing)
										connect();
								}
							}
						}, 5, TimeUnit.SECONDS);
					}
					catch ( IOException e )
					{
						plugin.getLogger().severe("Unable to connect to Slack service:");
						e.printStackTrace();
						session = null;
					}
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
		synchronized(lockObject)
		{
			session = null;
			if (!isClosing)
			{
				ProxyServer.getInstance().getScheduler().schedule(plugin, new Runnable()
				{
					@Override
					public void run()
					{
						synchronized(lockObject)
						{
							if (!isClosing)
								connect();
						}
					}
				}, 5, TimeUnit.SECONDS);
			}
		}
	}
	
}
