package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class ProxyCompleteMission extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public ProxyCompleteMission() {
        id = UUID.randomUUID();
    }

    public ProxyCompleteMission(UUID missionId, ProxyInt proxy) {
        this.missionId = missionId;
        id = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "ProxyCompleteMission [" + missionId + "]";
    }
}
