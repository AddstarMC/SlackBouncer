package au.com.addstar.slackbouncer.commands;

import io.github.slackapi4j.MessageOptions;
import net.cubespace.geSuit.managers.AdminCommandManager;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 8/08/2017.
 */
public class AdminCommandHandler implements ISlackCommandHandler {
    @Override
    public String getUsage(String command) {
        return null;
    }

    @Override
    public void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException {
        if (!sender.isSlackAdmin()){
            sender.sendMessage("You cannot use this slack command as you are NOT a Slack Admin.", MessageOptions.DEFAULT);
        }
        switch (command) {
            case "restart":
                onAdminRestart(sender,args);
                break;
        }
    }

    private void onAdminRestart(SlackCommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Usage: restart <servername> <time> (e.g. 2h3m5s", MessageOptions.DEFAULT);
            return;
        }
        String server = args[0];
        String time = args[1];
        AdminCommandManager.sendAdminCommand(sender, server, "restart", time);
        sender.sendMessage("Administrative Restart was sent to " + server + " to execute in "+time, MessageOptions.DEFAULT );
    }
}
