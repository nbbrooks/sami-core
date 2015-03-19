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
    // Fields
    public String interruptName;

    static {
        fieldNames.add("interruptName");

        fieldNameToDescription.put("interruptName", "Interrupt name?");
    }

    public OperatorInterruptReceived() {
    }

    public OperatorInterruptReceived(UUID relevantOutputEventUuid, UUID missionUuid, UUID relevantOutputEventId, String interruptName) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.interruptName = interruptName;
        // There should be no place preceding the transition containing the OperatorInterruptReceived
        //  Define a relevantOutputEventId so it can be used to distinguish between different OperatorInterruptReceived events within the same mission specification
        this.relevantOutputEventId = relevantOutputEventId;
        id = UUID.randomUUID();
    }

    public String getInterruptName() {
        return interruptName;
    }

    public String toString() {
        return "OperatorInterruptReceived [" + interruptName + "]";
    }
}
