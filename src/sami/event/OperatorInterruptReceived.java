package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class OperatorInterruptReceived extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    protected UUID correspondingOirEventId;
    // Fields
    public String interruptName;

    static {
        fieldNames.add("interruptName");

        fieldNameToDescription.put("interruptName", "Interrupt name?");
    }

    public OperatorInterruptReceived() {
        id = UUID.randomUUID();
    }

    public OperatorInterruptReceived(UUID relevantOutputEventUuid, UUID missionUuid, UUID correspondingOirEventId, String interruptName) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.interruptName = interruptName;
        //  Define a correspondingOirEventId so it can be used to distinguish between different OperatorInterruptReceived events within the same mission specification
        this.correspondingOirEventId = correspondingOirEventId;
        id = UUID.randomUUID();
    }

    public String getInterruptName() {
        return interruptName;
    }

    public UUID getOirId() {
        return correspondingOirEventId;
    }

    public String toString() {
        return "OperatorInterruptReceived [" + interruptName + "]";
    }
}
