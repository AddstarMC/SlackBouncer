package au.com.addstar.slackbouncer.bouncers;

import au.com.addstar.slackapi.RealTimeSession;
import au.com.addstar.slackapi.SlackAPI;
import au.com.addstar.slackapi.events.MessageEvent;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

public interface ISlackIncomingBouncer
{
	public void load(ConfigSection section) throws InvalidConfigurationException;
	
	public void initialize(RealTimeSession session, SlackAPI api);
	
	public void onMessage(MessageEvent event);
}
