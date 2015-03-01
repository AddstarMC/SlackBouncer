package au.com.addstar.slackbouncer.commands;

public interface ISlackCommandHandler
{
	public void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException;
}
