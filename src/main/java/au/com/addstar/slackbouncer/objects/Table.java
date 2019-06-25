package au.com.addstar.slackbouncer.objects;

/**
 * Created for the Charlton IT Project.
 * Created by benjicharlton on 25/06/2019.
 */
import java.util.HashMap;
import java.util.Map;

public enum Table {
    IDEA("SHT_Ideas", "idea"),
    TICKET("SHT_Tickets", "ticket");

    private static final Map<String, Table> BY_TYPE = new HashMap();
    private static final Map<String, Table> BY_TABLENAME = new HashMap();

    static {
        Table[] var3 = values();
        Table[] var1 = var3;
        int var2 = var3.length;

        for(int i = 0; i < var2; ++i) {
            Table table = var1[i];
            BY_TYPE.put(table.type, table);
            BY_TABLENAME.put(table.tableName, table);
        }

    }

    public String tableName;
    public String type;

    private Table(String tableName, String name) {
        this.tableName = tableName;
        this.type = name;
    }

    public static Table matchIdentifier(String id) {
        return BY_TYPE.get(id);
    }

    public static Table matchTableName(String tablename) {
        return BY_TABLENAME.get(tablename);
    }
}
