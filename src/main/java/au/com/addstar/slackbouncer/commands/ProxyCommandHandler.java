package au.com.addstar.slackbouncer.commands;

import au.com.addstar.slackbouncer.BouncerPlugin;
import au.com.addstar.slackbouncer.bouncers.MonitorBouncer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import io.github.slackapi4j.objects.Message;
import io.github.slackapi4j.objects.blocks.Block;
import io.github.slackapi4j.objects.blocks.Divider;
import io.github.slackapi4j.objects.blocks.Section;
import io.github.slackapi4j.objects.blocks.composition.TextObject;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProxyCommandHandler implements ISlackCommandHandler {
  private final BouncerPlugin plugin;

  public ProxyCommandHandler(BouncerPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getUsage(String command) {
    switch (command) {
      case "list":
      case "who":
        return command + " -  list all players on servers.";
      case "watch":
      case "monitor":
        return command + " - start monitoring a player for login and out.";
      default:
        return command;
    }
  }

  @Override
  public void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException {
    switch (command.toLowerCase()) {
      case "who":
      case "list":
        onWho(sender);
        break;
      case "monitor":
      case "watch":
        if (args.length < 1) {
          throw new IllegalArgumentException("Please add a player to add");
        }
        String pName = args[0];
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
        if (player != null) {
          MonitorBouncer.addWatched(player, sender.getUser());
        } else {
          sender.sendMessage("Player not found");
        }
    }
  }

  public void onWho(SlackCommandSender sender) {
    Collection<ProxiedPlayer> players = ProxyServer.getInstance().getPlayers();
    List<Block> messageBlocks = new ArrayList<>();
    Section section = new Section();
    TextObject title = TextObject.builder().text((players.size() > 0 ? ":warning: " : ":information_source: ") +
        players.size() + " players online").type(TextObject.TextType.MARKDOWN).build();
    section.setText(title);
    messageBlocks.add(section);
    messageBlocks.add(new Divider());
    ListMultimap<String, String> groups = ArrayListMultimap.create();
    for (ProxiedPlayer player : players) {
      String serverName;
      if (player.getServer() != null) {
        serverName = player.getServer().getInfo().getName();
      } else {
        serverName = "Not Joined";
      }

      groups.put(serverName, player.getDisplayName());
    }
    List<String> sortedKeys = Lists.newArrayList(groups.keySet());
    Collections.sort(sortedKeys);
    for (String key : sortedKeys) {
      List<String> groupPlayers = Lists.newArrayList(groups.get(key));
      Collections.sort(groupPlayers);
      Section server = new Section();
      server.setText(TextObject.builder().text(String.format("%s (%d players)",
          key, groupPlayers.size())).build());
      List<TextObject> player = groupPlayers
          .stream()
          .sorted()
          .map(input -> TextObject.builder().text(input).build())
          .collect(Collectors.toList());
      server.setFields(player);
      messageBlocks.add(server);
    }
    Message message = sender.createSlackMessage();
    message.setBlocks(messageBlocks);
    message.setText("Who");
    sender.sendMessage(message);

  }

  private Function<String, TextObject> getFunc() {
    return input -> TextObject.builder().text(input).build();
  }
}
