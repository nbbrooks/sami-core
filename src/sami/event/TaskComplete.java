package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Used to indicate that the task a proxy was executing has been finished and
 * the proxy can begin its next task's events
 *
 * @author nbb
 */
public class TaskComplete extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();

    public TaskComplete() {
        id = UUID.randomUUID();
    }

    public TaskComplete(UUID uuid, UUID missionUuid) {
        this.id = uuid;
        this.missionId = missionUuid;
    }
}
