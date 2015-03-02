package au.com.addstar.slackbouncer.commands;

public interface ISlackCommandHandler
{
	public String getUsage(String command);
	public void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException;
}
