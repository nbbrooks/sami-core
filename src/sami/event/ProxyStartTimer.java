package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class ProxyStartTimer extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public int timerDuration;
    
    static {
        fieldNames.add("timerDuration");

        fieldNameToDescription.put("timerDuration", "Proxy timer duration? (s)");
    }
    
    public ProxyStartTimer() {
        id = UUID.randomUUID();
    }

    public ProxyStartTimer(UUID missionId, ProxyInt proxy) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }

    public String toString() {
        return "ProxyStartTimer [" + timerDuration + "]";
    }
}
