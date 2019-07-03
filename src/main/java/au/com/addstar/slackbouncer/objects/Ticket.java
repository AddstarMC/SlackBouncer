package au.com.addstar.slackbouncer.objects;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 25/06/2019.
 */
public class Ticket {

        private final UUID owner;
        private final String description;
        private final LocalDateTime createdDate;
        private final TicketLocation location;
        private Integer id;
        private String ownerName = "";
        private String adminReply = "NONE";
        private String userReply = "NONE";
        private Status status;
        private String admin;
        private Timestamp expirationDate;


        public Ticket(int id, UUID owner, String description, LocalDateTime createdDate, TicketLocation location) {
            this.status = Status.OPEN;
            this.admin = "";
            this.id = id;
            this.owner = owner;
            this.description = description;
            this.createdDate = createdDate;
            this.location = location;
        }


        public Ticket(UUID owner, String description, LocalDateTime createdDate, TicketLocation location) {
            this.status = Status.OPEN;
            this.admin = "";
            this.owner = owner;
            this.description = description;
            this.createdDate = createdDate;
            this.location = location;
        }

        public Integer getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public UUID getOwner() {
            return this.owner;
        }

        public String getDescription() {
            return this.description;
        }

        public LocalDateTime getCreatedDate() {
            return this.createdDate;
        }

        public String getOwnerName() {
            return this.ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public TicketLocation getLocation() {
            return this.location;
        }

        public String getAdminReply() {
            return this.adminReply;
        }

        public void setAdminReply(String adminReply) {
            this.adminReply = adminReply;
        }

        public String getUserReply() {
            return this.userReply;
        }

        public void setUserReply(String userReply) {
            this.userReply = userReply;
        }

        public Status getStatus() {
            return this.status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getAdmin() {
            return this.admin;
        }

        public void setAdmin(String admin) {
            this.admin = admin;
        }

        public Timestamp getExpirationDate() {
            return this.expirationDate;
        }

        public void setExpirationDate(Timestamp expirationDate) {
            this.expirationDate = expirationDate;
        }

        public boolean isOpen() {
            return this.status == Ticket.Status.OPEN;
        }

        public boolean hasAdminReply() {
            return !this.adminReply.equalsIgnoreCase("NONE");
        }

        public boolean hasUserReply() {
            return !this.userReply.equalsIgnoreCase("NONE");
        }

        @SuppressWarnings("unused")
        public enum Status {
            OPEN,
            CLOSE;

            Status() {
            }
        }
}
