package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class ProxyAbortMission extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public ProxyInt proxy;

    static {
        fieldNames.add("proxy");

        fieldNameToDescription.put("proxy", "Proxy to finish plan via abort? (s)");
    }

    public ProxyAbortMission() {
        id = UUID.randomUUID();
    }

    public ProxyAbortMission(UUID missionId, ProxyInt proxy) {
        this.missionId = missionId;
        this.proxy = proxy;
        id = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "ProxyAbortMission [" + missionId + ", " + proxy + "]";
    }
}
