package au.com.addstar.slackbouncer.config;

import com.google.common.collect.Maps;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.YamlConfig;

import java.util.Map;

public class ChannelDefinition extends YamlConfig {
  public ChannelDefinition() {
    incoming = Maps.newHashMap();
    outgoing = Maps.newHashMap();
  }

  public final Map<String, ConfigSection> incoming;
  public final Map<String, ConfigSection> outgoing;

  @Override
  public String toString() {
    return String.format("{Incoming: %s Outgoing: %s}", incoming, outgoing);
  }
}
