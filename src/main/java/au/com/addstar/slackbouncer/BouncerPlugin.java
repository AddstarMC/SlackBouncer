package au.com.addstar.slackbouncer;

import au.com.addstar.slackbouncer.bouncers.AdminBouncer;
import au.com.addstar.slackbouncer.bouncers.BungeeChatBouncer;
import au.com.addstar.slackbouncer.bouncers.GeSuitBouncer;
import au.com.addstar.slackbouncer.bouncers.ISlackIncomingBouncer;
import au.com.addstar.slackbouncer.bouncers.ISlackOutgoingBouncer;
import au.com.addstar.slackbouncer.bouncers.MonitorBouncer;
import au.com.addstar.slackbouncer.commands.AdminCommandHandler;
import au.com.addstar.slackbouncer.commands.GeSuitCommandHandler;
import au.com.addstar.slackbouncer.commands.ISlackCommandHandler;
import au.com.addstar.slackbouncer.commands.ProxyCommandHandler;
import au.com.addstar.slackbouncer.commands.SlackCommandSender;
import au.com.addstar.slackbouncer.commands.TicketCommandHandler;
import au.com.addstar.slackbouncer.config.ChannelDefinition;
import au.com.addstar.slackbouncer.config.MainConfig;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.github.slackapi4j.events.MessageEvent;
import io.github.slackapi4j.exceptions.SlackException;
import io.github.slackapi4j.objects.Conversation;
import io.github.slackapi4j.objects.Message;
import io.github.slackapi4j.objects.ObjectID;
import io.github.slackapi4j.objects.User;
import io.github.slackapi4j.objects.blocks.Block;
import io.github.slackapi4j.objects.blocks.Divider;
import io.github.slackapi4j.objects.blocks.Section;
import io.github.slackapi4j.objects.blocks.composition.TextObject;
import net.cubespace.Yamler.Config.ConfigSection;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BouncerPlugin extends Plugin {
  private final Map<String, Constructor<? extends ISlackIncomingBouncer>> incomingRegistrations;
  private final Map<String, Constructor<? extends ISlackOutgoingBouncer>> outgoingRegistrations;
  private final Map<String, ISlackCommandHandler> commandHandlers;

  private MainConfig config;
  private Bouncer bouncer;
  //private MonitorBouncer monitor;

  private final List<BouncerChannel> channels;
  private final WeakHashMap<ObjectID, Conversation> conversations;//this is a list of conversations that were not found by in the Session

  public BouncerPlugin() {
    incomingRegistrations = Maps.newHashMap();
    outgoingRegistrations = Maps.newHashMap();
    commandHandlers = Maps.newHashMap();
    channels = Lists.newArrayList();
    conversations = new WeakHashMap<>();
  }

  @Override
  public void onEnable() {
    config = new MainConfig(new File(getDataFolder(), "config.yml"));
    if (getProxy().getPluginManager().getPlugin("BungeeChat") != null) {
      registerIncomingBouncer(BungeeChatBouncer.class);
      registerOutgoingBouncer("bungeechat", BungeeChatBouncer.class);
    }

    if (getProxy().getPluginManager().getPlugin("geSuit") != null) {
      registerOutgoingBouncer("gesuit", GeSuitBouncer.class);
      registerOutgoingBouncer("admin", AdminBouncer.class);
      registerCommandHandler(new GeSuitCommandHandler(), "seen", "where", "names", "warnhistory", "banhistory", "geo", "ban", "unban");
      registerCommandHandler(new AdminCommandHandler(), "restart");
    }

    registerCommandHandler(new ProxyCommandHandler(this), "who", "list", "monitor", "watch");
    registerOutgoingBouncer("monitor", MonitorBouncer.class);

    if (!loadConfig()) {
      return;
    }
    ConfigSection section = config.ticketConfig;
    Boolean ticketEnable = section.get("enabled");
    if (ticketEnable) {
      registerCommandHandler(new TicketCommandHandler(section), "tickets", "ideas", "reply");
    } else {
      getLogger().info("TicketManager is disabled via configuration.");
    }

    getProxy().getPluginManager().registerCommand(this, new BouncerCommand(this));
    tryStartBouncer();
  }

  @Override
  public void onDisable() {
    if (bouncer != null) {
      bouncer.shutdown();
      bouncer = null;
    }
  }

  private boolean loadConfig() {
    try {
      config.init();

      loadChannels();
      return true;
    } catch (Exception e) {
      getLogger().severe("Unable to load configuration: " + e.getMessage());
      return false;
    }
  }

  private boolean tryStartBouncer() {
    if (Strings.isNullOrEmpty(config.token) || config.token.equals("*unspecified*")) {
      getLogger().severe("Token is not configured. Please edit the config and set the token");
      return false;
    }

    bouncer = new Bouncer(this);

    return true;
  }

  private void loadChannels() {
    channels.clear();
    for (Entry<String, ChannelDefinition> entry : config.channels.entrySet()) {
      BouncerChannel channel = new BouncerChannel(entry.getKey(), this);
      channel.load(entry.getValue());
      channels.add(channel);
    }
  }

  protected boolean reloadConfig() {
    if (bouncer != null) {
      bouncer.shutdown();
      bouncer = null;
    }

    return loadConfig() && tryStartBouncer();

  }

  private void registerIncomingBouncer(Class<? extends ISlackIncomingBouncer> bouncerClass) {
    try {
      Constructor<? extends ISlackIncomingBouncer> constructor = bouncerClass.getConstructor();
      incomingRegistrations.put("bungeechat".toLowerCase(), constructor);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(bouncerClass.getName() + " does not have a public default constructor");
    }
  }

  private void registerOutgoingBouncer(String name, Class<? extends ISlackOutgoingBouncer> bouncerClass) {
    try {
      Constructor<? extends ISlackOutgoingBouncer> constructor = bouncerClass.getConstructor(BouncerChannel.class);
      outgoingRegistrations.put(name.toLowerCase(), constructor);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(bouncerClass.getName() + " does not have a public constructor that takes a BouncerChannel");
    }
  }

  public void registerCommandHandler(ISlackCommandHandler handler, String... commands) {
    for (String command : commands)
      commandHandlers.put(command.toLowerCase(), handler);
  }

  public MainConfig getConfig() {
    return config;
  }

  ISlackIncomingBouncer makeIncomingBouncer(String name) {
    Constructor<? extends ISlackIncomingBouncer> constructor = incomingRegistrations.get(name.toLowerCase());
    if (constructor == null) {
      return null;
    }
    try {
      return constructor.newInstance();
    } catch (IllegalArgumentException | IllegalAccessException e) {
      // Should never happen
      throw new AssertionError(e);
    } catch (InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  ISlackOutgoingBouncer makeOutgoingBouncer(String name, BouncerChannel channel) {
    Constructor<? extends ISlackOutgoingBouncer> constructor = outgoingRegistrations.get(name.toLowerCase());
    if (constructor == null) {
      return null;
    }

    try {
      return constructor.newInstance(channel);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      // Should never happen
      throw new AssertionError(e);
    } catch (InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public Bouncer getBouncer() {
    return bouncer;
  }

  void onLoginComplete() {
    for (BouncerChannel bChannel : channels) {
      Conversation slackChannel = bouncer.getSession().getChannel(bChannel.getName());

      if (slackChannel == null) {
        getLogger().warning("Unable to join non-existant channel " + bChannel.getName());
        continue;
      }

      // Check that the client is in the channel (join if not)
      if (!slackChannel.isMember()) {
        getLogger().severe("Not a member of group " + bChannel.getName() + ". Must be invited into group");
      } else {       // Is already a member, link the channel
        bChannel.link(slackChannel);
      }
    }
  }

  void onMessage(MessageEvent event) {
    // DEBUGGING: Trying to find out what was null in that same user check
    if (event.getUser() == null) {
      getLogger().info("[DEBUG] Event user was null: " + event.toString());
      return;
    }

    if (bouncer == null) {
      getLogger().info("[DEBUG] bouncer was null");
      return;
    }

    if (bouncer.getSession() == null) {
      getLogger().info("[DEBUG] session was null");
      return;
    }

    if (event.getUser().equals(bouncer.getSession().getSelf())) {
      return;
    }

    if (event.getMessage().getText() == null) {
      return;
    }

    String message = SlackUtils.resolveGroups(event.getMessage().getText(), bouncer.getSession());
    ObjectID convID = event.getMessage().getConversationID();
    final Conversation source;
    try {
      source = retrieveConversation(convID);
    } catch (NullPointerException e) {
      getLogger().info(e.getMessage());
      return;
    }
    // Command handling
    if (event.getType() == Message.MessageType.Normal) {
      String user = bouncer.getSession().getSelf().getName().toLowerCase();

      if (source.isIm() || (message.toLowerCase().startsWith(user) || message.toLowerCase().startsWith("@" + user))) {
        processCommands(event.getUser(), source, message);
        return;
      }
    }

    // Process bouncers
    for (BouncerChannel channel : channels) {
      if (channel.getSlackChannel() == null) {
        continue;
      }

      if (channel.getSlackChannel().getId().equals(event.getMessage().getConversationID())) {
        channel.onMessage(message, event.getUser(), event.getType());
      }
    }
  }

  @NotNull
  private Conversation retrieveConversation(ObjectID convID) throws NullPointerException {
    conversations.clear();
    Conversation source = bouncer.getSession().getChannelById(convID);
    //its not cached so lets retrieve it
    if (source == null) {//for some reason the session wont have the DM channel if the user opens one in the session.
      source = conversations.get(convID);
      if (source == null) {
        try {
          source = bouncer.getSlack().getConversations().getConversation(convID.toString());
          if (source == null) {
            getLogger().info("[DEBUG] Channel was null");
            throw new NullPointerException("Source Conversation was null");
          }
          conversations.put(convID, source);
        } catch (IOException | SlackException e) {
          getLogger().info(convID + " could not be retrieved from Slack: " + e.getMessage());
          NullPointerException npe = new NullPointerException("Source Conversation was null: " + e.getMessage());
          npe.addSuppressed(e);
          throw npe;
        }
      }
    }
    return source;
  }

  private void processCommands(User source, Conversation channel, String text) {
    int start;

    SlackCommandSender sender = new SlackCommandSender(this, bouncer, source, channel);

    String[] arguments = text.split(" ");

    if (arguments.length == 0) {
      return;
    }

    String user = bouncer.getSession().getSelf().getName().toLowerCase();

    if (arguments[0].toLowerCase().startsWith(user) || arguments[0].toLowerCase().startsWith("@" + user)) {
      start = 1;
    } else {
      start = 0;
    }

    if (arguments.length < start + 1) {

      sender.sendEphemeral(Message.builder()
          .userId(sender.getUser().getId())
          .conversationID(channel.getId())
          .text("You did not give me a command").build());
      return;
    }

    String command = arguments[start];

    if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("commands")) {
      List<Block> messageBlocks = new ArrayList<>();
      Section section = new Section();
      TextObject title = TextObject.builder()
          .type(TextObject.TextType.MARKDOWN).text("List of commands")
          .build();
      section.setText(title);
      messageBlocks.add(section);
      messageBlocks.add(new Divider());
      Section subsect = new Section();
      TextObject subHead = TextObject.builder()
          .text("The following commands are available").build();
      subsect.setText(subHead);
      messageBlocks.add(subsect);
      Section helpText = new Section();
      final StringBuilder commandHelp = new StringBuilder();
      final AtomicInteger i = new AtomicInteger(1);
      commandHandlers.keySet().stream().sorted().forEach(s -> {
        ISlackCommandHandler handler = commandHandlers.get(s);
        commandHelp.append(i.get()).append(". ").append(handler.getUsage(s)).append(System.lineSeparator());
        i.incrementAndGet();
      });
      helpText.setText(TextObject.builder().text(commandHelp.toString()).build());
      messageBlocks.add(helpText);
      messageBlocks.add(new Divider());
      Message out = Message.builder()
          .conversationID(channel.getId())
          .userId(sender.getUser().getId())
          .text("Command Help")
          .blocks(messageBlocks)
          .build();
      sender.sendEphemeral(out);
      return;
    }

    ISlackCommandHandler handler = commandHandlers.get(command.toLowerCase());
    if (handler == null) {
      Message message = Message.builder()
          .userId(sender.getUser().getId())
          .conversationID(channel.getId())
          .subtype(Message.MessageType.Normal)
          .text("I dont know what to do with _" + command + "_")
          .build();
      try {
        bouncer.getSlack().sendEphemeral(message);
      } catch (IOException | SlackException e) {
        e.printStackTrace();
      }
      //sender.sendMessage("I dont know what to do with _" + command + "_");
      return;
    }

    arguments = Arrays.copyOfRange(arguments, start + 1, arguments.length);

    try {
      handler.onCommand(sender, command, arguments);
    } catch (IllegalArgumentException e) {
      sender.sendMessage(e.getMessage());
    } catch (IllegalStateException e) {
      sender.sendMessage("Usage: " + e.getMessage());
    }
  }
}
