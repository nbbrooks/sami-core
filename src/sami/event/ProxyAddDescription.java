package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ProxyAddDescription extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public String description;

    static {
        fieldNames.add("description");

        fieldNameToDescription.put("description", "Proxy description?");
    }

    public ProxyAddDescription() {
        id = UUID.randomUUID();
    }

    public ProxyAddDescription(UUID uuid, UUID missionUuid, String description) {
        this.id = uuid;
        this.missionId = missionUuid;
        this.description = description;
    }

    public String toString() {
        return "ProxyAddDescription [" + description + "]";
    }
}
