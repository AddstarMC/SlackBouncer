package au.com.addstar.slackbouncer.bouncers;

import au.com.addstar.slackapi.objects.Message.MessageType;
import au.com.addstar.slackapi.objects.User;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

public interface ISlackIncomingBouncer
{
	void load(ConfigSection section) throws InvalidConfigurationException;
	
	void onMessage(String message, User sender, MessageType type);
}
