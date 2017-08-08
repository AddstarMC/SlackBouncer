package au.com.addstar.slackbouncer.commands;

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
        switch (command) {
            case "restart":
                onAdminRestart(sender,args);
                break;
        }
    }

    private void onAdminRestart(SlackCommandSender sender, String[] args){
        if(args.length != 2){
            sender.sendMessage("Usage: restart <servername> <time> (e.g. 2h3m5s");
            return;
        }

    }
}
