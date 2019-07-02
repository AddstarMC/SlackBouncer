package au.com.addstar.slackbouncer.bouncers;

import java.util.*;

import au.com.addstar.slackbouncer.BouncerChannel;
import io.github.slackapi4j.objects.User;
import net.cubespace.Yamler.Config.ConfigSection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Created for the AddstarMC Project. Created by Narimm on 19/02/2019.
 */
public class MonitorBouncer implements ISlackOutgoingBouncer, Listener {
    private static Map<ProxiedPlayer, User> watched = new HashMap<>();
    private BouncerChannel channel;
    private boolean enabled;
    
    public MonitorBouncer(BouncerChannel channel) {
        this.channel = channel;
    }
    
    @EventHandler
    public void onPlayerLogin(PostLoginEvent event){
        if(!enabled)return;
        ProxiedPlayer player = event.getPlayer();
        if(watched.containsKey(player)){
            String message = player.getDisplayName() + " has logged into the server";
            if(watched.get(player) == null){
                channel.sendMessage(message);
            }else{
                User user = watched.get(player);
                channel.sendMessage(message,user);
            }
            }
        }
        
        public Map<String,String> getWatched(){
            if(!enabled)return null;
            Map<String,String> res = new HashMap<>();
            for(Map.Entry<ProxiedPlayer,User> e:watched.entrySet()){
                res.put(e.getKey().getDisplayName(),e.getValue().getName());
            }
            return res;
        }
    
    public static void addWatched(ProxiedPlayer player, User sender){
        watched.put(player,sender);
    }
    @Override
    public void load(ConfigSection section){
        enabled = section.has("monitor") && section.<Boolean>get("monitor");
    }
}
