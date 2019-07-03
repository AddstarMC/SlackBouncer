package au.com.addstar.slackbouncer.objects;

public class TicketLocation {
    private final double x;
    private final double y;
    private final double z;
    private final String world;
    private final float pitch;
    private final float yaw;
    private final String server;

    public TicketLocation(Double x, Double y, Double z, String world, Float pitch, Float yaw, String server) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.pitch = pitch;
        this.yaw = yaw;
        this.server = server;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public String getWorld() {
        return this.world;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public String getServer() {
        return this.server;
    }
}
