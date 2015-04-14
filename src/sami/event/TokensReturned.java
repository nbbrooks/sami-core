package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class TokensReturned extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public TokensReturned() {
        id = UUID.randomUUID();
    }

    public TokensReturned(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<ProxyInt> proxyList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        if (proxyList != null) {
            relevantProxyList = new ArrayList<ProxyInt>();
            relevantProxyList.addAll(proxyList);
        }
    }

    public String toString() {
        return "TokensReturned [" + (relevantProxyList != null ? relevantProxyList.toString() : "null") + "]";
    }
}
