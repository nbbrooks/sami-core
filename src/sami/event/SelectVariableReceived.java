package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.variable.VariableReference;

/**
 *
 * @author nbb
 */
public class SelectVariableReceived extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Variables
    public VariableReference variableReference = null;

    static {
        variableNames.add("variableReference");

        variableNameToDescription.put("variableReference", "Name of selected variable.");
    }

    public SelectVariableReceived() {
        id = UUID.randomUUID();
    }

    public SelectVariableReceived(UUID relevantOutputEventId, UUID missionId, VariableReference variableReference) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.variableReference = variableReference;
        id = UUID.randomUUID();
    }

    public VariableReference getVariableReference() {
        return variableReference;
    }

    public String toString() {
        return "SelectVariableReceived [" + (variableReference != null ? variableReference.toString() : "null") + "]";
    }
}
