package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.variable.VariableClass;

/**
 *
 * @author nbb
 */
public class SelectVariableRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public VariableClass variableClass;

    static {
        fieldNames.add("variableClass");

        fieldNameToDescription.put("variableClass", "Class the selected variable should be?");
    }

    public SelectVariableRequest() {
        id = UUID.randomUUID();
    }

    public SelectVariableRequest(UUID missionId, VariableClass variablesClassToSelect) {
        this.missionId = missionId;
        this.variableClass = variablesClassToSelect;
        id = UUID.randomUUID();
    }

    public VariableClass getVariableClass() {
        return variableClass;
    }

    public String toString() {
        return "SelectVariableRequest [" + variableClass + "]";
    }
}
