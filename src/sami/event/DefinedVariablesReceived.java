package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class DefinedVariablesReceived extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Fields
    protected Hashtable<String, Object> variableNameToDefinition;

    public DefinedVariablesReceived() {
        id = UUID.randomUUID();
    }

    public DefinedVariablesReceived(UUID relevantOutputEventId, UUID missionId, Hashtable<String, Object> variableNameToDefinition) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.variableNameToDefinition = variableNameToDefinition;
        id = UUID.randomUUID();
    }

    public Hashtable<String, Object> getVariableNameToDefinition() {
        return variableNameToDefinition;
    }

    public String toString() {
        return "DefinedVariablesReceived [" + (variableNameToDefinition != null ? variableNameToDefinition.toString() : "null") + "]";
    }
}
