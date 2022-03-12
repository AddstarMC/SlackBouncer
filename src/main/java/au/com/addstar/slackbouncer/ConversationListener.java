package au.com.addstar.slackbouncer;

import io.github.slackapi4j.RealTimeSession;
import io.github.slackapi4j.eventlisteners.ConversationEventListener;
import io.github.slackapi4j.events.ConversationEvent;
import io.github.slackapi4j.events.UserConversationEvent;
import io.github.slackapi4j.exceptions.SlackException;
import io.github.slackapi4j.exceptions.SlackRtException;
import io.github.slackapi4j.objects.Conversation;
import io.github.slackapi4j.objects.Message;
import io.github.slackapi4j.objects.ObjectID;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 3/07/2019.
 */
public class ConversationListener extends ConversationEventListener {

  private final RealTimeSession session;
  private final Logger log;
  private final BouncerPlugin plugin;

  public ConversationListener(BouncerPlugin plugin) {
    this.plugin = plugin;
    this.session = plugin.getBouncer().getSession();
    this.log = plugin.getLogger();
    session.addListener(this);
  }

  @Override
  public void onLoginComplete() {
    plugin.getLogger().info("Logged into Slack as " + session.getSelf().getName());
    plugin.onLoginComplete();
    plugin.getLogger().info("Listening in " + session.getAllChannels().size() + " Slack channels.");
    /*for (Conversation c : session.getAllChannels()) {
      plugin.getLogger().info("  - " + c.getName() + " (" + c.getNumMembers() + " members)");
    }*/
  }

  @Override
  public void onError(SlackRtException cause) {
    log.warning("Conversation Event Exception: " + cause.getLocalizedMessage());
  }

  @Override
  public void onClose() {
    log.info("Slack connection closed.");
  }

  @Override
  public void onConversation(ConversationEvent event) {

    ConversationEvent.EventType type = event.getType();
    switch (type) {
      case Join:
      case Open:
        if (event instanceof UserConversationEvent) {
          ObjectID userID = ((UserConversationEvent) event).getUserID();
          Message message = Message.builder()
              .conversationID(event.getConversationID())
              .userId(userID)
              .text("<@" + userID.getId() + "> I can process commands for you in this channel just use @"
                  + session.getSelf().getName())
              .build();
          try {
            session.getApi().sendEphemeral(message);
          } catch (IOException | SlackException e) {
            log.severe(e.getLocalizedMessage());
          }
        }
        break;
      default:
        //no handler
    }
  }
}
