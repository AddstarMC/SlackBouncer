package au.com.addstar.slackbouncer.commands;


import au.com.addstar.slackbouncer.database.MySQLConnection;
import au.com.addstar.slackbouncer.managers.SimpleTicketManager;
import au.com.addstar.slackbouncer.objects.Table;
import au.com.addstar.slackbouncer.objects.Ticket;
import io.github.slackapi4j.objects.Message;
import io.github.slackapi4j.objects.blocks.Section;
import io.github.slackapi4j.objects.blocks.composition.TextObject;
import net.cubespace.Yamler.Config.ConfigSection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 16/05/2019.
 */
public class TicketCommandHandler implements ISlackCommandHandler {
    private SimpleTicketManager manager;
    private boolean enabled;

    public TicketCommandHandler(ConfigSection parent)   {
        ConfigSection section = parent.get("MYSQL");
        String host = (section.has("hostname"))?section.get("hostname"):"localhost";
        String port = (section.has("hostport"))?section.get("hostport").toString():"3306";
        String dbName = (section.has("database"))?section.get("database"):"simpletickets";
        Properties properties = new Properties();
        ConfigSection props =  (section.has("database"))?section.get("properties"):new ConfigSection();
        properties.put("useSSL", props.has("useSSL")?props.get("useSSL").toString():"false");
        Logger log = Logger.getLogger("SlackBouncer");
        MySQLConnection database = null;
        try {
            database = new MySQLConnection(host, port,dbName, properties);
            manager = new SimpleTicketManager(database,log,false);
            enabled = true;
            log.info("TicketManager has been configured - Ticket management is available");
        }catch (SQLException e) {
            enabled =false;
            log.info("TicketManager has not be able to configure a database connection - Ticket management is NOT available");
            log.severe(e.getMessage());
        }
    }

    @Override
    public String getUsage(String command) {
        return "tickets | reply <id> <response>";
    }

    @Override
    public void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException {
        try {
            if(!enabled){
                notEnabled(sender);
                return;
            }
            switch (command.toLowerCase()) {
                case "tickets":
                    listTickets(sender);
                    break;
                case "reply":
                    throw new IllegalArgumentException("Not yet Supported");
            }
        }catch (Exception e){
            if(e instanceof IllegalArgumentException)
                throw new IllegalArgumentException(e);
            throw new IllegalStateException(e);
        }
    }
    private void notEnabled(SlackCommandSender sender){
        Message message = sender.createSlackMessage();
        message.setText("The ticket manager is not available.");
        sender.sendEphemeral(message);
    }
    private void listTickets(SlackCommandSender sender){
        Table table = SimpleTicketManager.getTableName("ticket");
        List<Ticket> tickets =  manager.getTickets(table, Ticket.Status.OPEN,5);
        Message message = sender.createSlackMessage();
        TextObject title = new TextObject();
        Section section = new Section();
        title.setText("*Open List of Tickets* (maximum 5 shown)");
        title.setType(TextObject.TextType.MARKDOWN);
        section.setText(title);
        message.addBlock(section);
        for(Ticket t:tickets){
            Section ticket = new Section();
            TextObject ticketTitle = new TextObject();
            ticketTitle.setText("*"+t.getId()+"* "+t.getDescription());
            ticket.setText(ticketTitle);
            List<TextObject> fields = new ArrayList<>();
            fields.add(createHeader("User"));
            fields.add(createHeader("Created"));
            fields.add(normalField(t.getOwnerName()));
            fields.add(normalField(t.getCreatedDate().toString()));
            fields.add(createHeader("Staff"));
            fields.add(createHeader("Reply"));
            fields.add(normalField(t.getAdmin()));
            fields.add(normalField(t.getAdminReply()));
            fields.add(createHeader("UserReply"));
            fields.add(createHeader("Expiry"));
            fields.add(normalField(t.getUserReply()));
            fields.add(normalField(t.getExpirationDate().toString()));
            ticket.setFields(fields);
            message.addBlock(ticket);
        }
        if(tickets.isEmpty()){
            Section empty = new Section();
            TextObject titleEmpty = new TextObject();
            titleEmpty.setText("No Tickets to display");
            empty.setText(titleEmpty);
            message.addBlock(empty);
        }
        sender.sendMessage(message);

    }
    private TextObject normalField(String string){
        TextObject text = new TextObject();
        text.setText(string);
        text.setType(TextObject.TextType.MARKDOWN);
        return text;
    }
    private TextObject createHeader(String string){
        TextObject text = new TextObject();
        text.setText("*"+string+"*");
        text.setType(TextObject.TextType.MARKDOWN);
        return text;
    }
}
