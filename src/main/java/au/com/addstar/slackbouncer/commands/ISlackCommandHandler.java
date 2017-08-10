package au.com.addstar.slackbouncer.commands;

public interface ISlackCommandHandler
{
	String getUsage(String command);
	void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException;
}
