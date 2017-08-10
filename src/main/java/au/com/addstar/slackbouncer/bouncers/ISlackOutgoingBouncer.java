package au.com.addstar.slackbouncer.bouncers;

import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

public interface ISlackOutgoingBouncer
{
	void load(ConfigSection section) throws InvalidConfigurationException;
}
