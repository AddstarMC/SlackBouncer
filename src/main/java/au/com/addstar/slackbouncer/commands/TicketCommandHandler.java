package au.com.addstar.slackbouncer.commands;

import au.com.addstar.slackapi.objects.Message;
import au.com.addstar.slackapi.objects.blocks.Section;
import au.com.addstar.slackapi.objects.blocks.composition.TextObject;
import au.com.addstar.slackbouncer.managers.SimpleTicketManager;
import me.odium.simplehelptickets.database.Database;
import me.odium.simplehelptickets.database.MySQLConnection;
import me.odium.simplehelptickets.database.Table;
import me.odium.simplehelptickets.manager.TicketManager;
import me.odium.simplehelptickets.objects.Ticket;
import net.cubespace.Yamler.Config.ConfigMapper;
import net.cubespace.Yamler.Config.ConfigSection;
import net.cubespace.Yamler.Config.InvalidConfigurationException;

import javax.naming.ConfigurationException;
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

    public TicketCommandHandler(ConfigSection parent) throws Exception  {
        if(!parent.has("MySQL"))
            throw new Exception("Could not locate configuration");
        ConfigSection section = parent.get("MySQL");
        String host = section.get("hostname");
        String port = section.get("hostport");
        String dbName = section.get("database");
        Properties properties = new Properties();
        ConfigSection props = section.get("properties");
        properties.put("useSSL", props.get("useSSL").toString());
        Logger log = Logger.getLogger("SlackBouncer");
        Database database = new MySQLConnection(host,port,properties,dbName,log);
        manager = new SimpleTicketManager(database,log,false);
    }

    @Override
    public String getUsage(String command) {
        return "tickets | reply <id> <response>";
    }

    @Override
    public void onCommand(SlackCommandSender sender, String command, String[] args) throws IllegalStateException, IllegalArgumentException {
        switch (command.toLowerCase())
        {
            case "tickets":
                listTickets(sender);
                break;
            case "reply":
                throw new IllegalArgumentException("Not yet Supported");
        }
    }

    private void listTickets(SlackCommandSender sender){
        Table table = TicketManager.getTableName("ticket");
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
