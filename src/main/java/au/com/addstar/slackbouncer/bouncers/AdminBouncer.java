package au.com.addstar.slackbouncer.bouncers;

import au.com.addstar.slackbouncer.BouncerChannel;
import net.cubespace.Yamler.Config.ConfigSection;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 8/08/2017.
 */
public class AdminBouncer implements ISlackOutgoingBouncer {
    private boolean restarts;
    private BouncerChannel mChannel;

    public AdminBouncer(BouncerChannel mChannel) {
        this.mChannel = mChannel;
    }

    @Override
    public void load(ConfigSection section) {
        restarts = section.has("allowRestarts") && section.<Boolean>get("allowRestarts");
    }
}
