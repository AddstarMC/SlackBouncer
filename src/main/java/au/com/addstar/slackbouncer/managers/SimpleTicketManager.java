package au.com.addstar.slackbouncer.managers;

import me.odium.simplehelptickets.database.Database;
import me.odium.simplehelptickets.database.Table;
import me.odium.simplehelptickets.objects.Ticket;
import me.odium.simplehelptickets.objects.TicketLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 24/06/2019.
 */
public class SimpleTicketManager {

    private final Database database;
    private final Logger log;
    private final boolean reminderIdeas;

    public SimpleTicketManager(Database database, Logger log, boolean reminderIdeas) {
        this.database = database;
        this.log = log;
        this.reminderIdeas = reminderIdeas;

    }

    public static Table getTableName(String identifier) {
        return Table.matchIdentifier(identifier);
    }
    public static Table getTargetItemName(String targetTable) {
        return Table.matchTableName(targetTable);
    }
    public static Table getTableFromCommandString(String commandString) {
        if (commandString.toLowerCase().contains("idea"))
            return Table.matchIdentifier("idea");
        else
            return Table.matchIdentifier("ticket");
    }
    public List<Ticket> getTickets(Table table, Ticket.Status status,int total) {
        String where;
        int param;
        where = " status = ?";
        param = 1;
        String limit;
        if(total > 0 )
            limit = " LIMIT "+total;
        else
            limit = "";
        String sql = "SELECT * FROM " + table.tableName + " WHERE " + where + limit ;
        List<Ticket> tickets = new ArrayList<>();
        Connection con = database.getConnection();
        if (con != null) {
            try {
                PreparedStatement statement = con.prepareStatement(sql);
                statement.setString(1, status.name());
                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    tickets.add(getFromResultRow(result));
                }
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            log.warning("Unable to get Database Connection");
        }
        return tickets;
    }
    private Ticket getFromResultRow(ResultSet result) throws SQLException {
        int id = result.getInt("id");
        String owner = result.getString("uuid");
        UUID uuid = null;
        try {
            uuid = UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            if (!owner.equalsIgnoreCase("CONSOLE")) {
                log.info(e.getMessage());
                e.printStackTrace();
            }
        }
        String server = result.getString("server");
        if (server == null) {
            server = "Unknown";
        }
        TicketLocation tL = new TicketLocation(result.getDouble("x"),
                        result.getDouble("y"),
                        result.getDouble("z"),
                        result.getString("world"),
                        result.getFloat("p"),
                        result.getFloat("f"),
                        server);


        String details = result.getString("description");

        java.sql.Timestamp sqlTimestamp = result.getTimestamp("date");
        Ticket ticket = new Ticket(id, uuid, details, sqlTimestamp.toLocalDateTime(), tL);
        String ownerName = result.getString("owner");
        ticket.setOwnerName(ownerName);
        ticket.setAdminReply(result.getString("adminreply"));
        ticket.setUserReply(result.getString("userreply"));
        Ticket.Status s;
        try {
            s = Ticket.Status.valueOf(result.getString("status"));
        } catch (IllegalArgumentException e) {
            s = Ticket.Status.OPEN;
        }
        ticket.setStatus(s);
        ticket.setExpirationDate(result.getTimestamp("expiration"));
        return ticket;
    }

}
