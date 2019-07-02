package au.com.addstar.slackbouncer.bouncers;

import io.github.slackapi4j.objects.Message;
import io.github.slackapi4j.objects.User;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

public interface ISlackIncomingBouncer
{
	void load(ConfigSection section) throws InvalidConfigurationException;
	
	void onMessage(String message, User sender, Message.MessageType type);
}
