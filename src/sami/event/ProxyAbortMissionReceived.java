package sami.event;

import sami.proxy.ProxyInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class ProxyAbortMissionReceived extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public ProxyAbortMissionReceived() {
        id = UUID.randomUUID();
    }

    public ProxyAbortMissionReceived(UUID missionId, ArrayList<ProxyInt> relevantProxyList) {
        this.missionId = missionId;
        this.relevantProxyList = relevantProxyList;
        id = UUID.randomUUID();
    }
}
