package au.com.addstar.slackbouncer.bouncers;

import au.com.addstar.slackapi.Message.MessageType;
import au.com.addstar.slackapi.User;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

public interface ISlackIncomingBouncer
{
	void load(ConfigSection section) throws InvalidConfigurationException;
	
	void onMessage(String message, User sender, MessageType type);
}
