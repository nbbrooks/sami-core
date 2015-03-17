package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import sami.variable.VariableName;

public class RedefineVariablesRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    protected Hashtable<VariableName, String> variableNameToDescription;
    public VariableName name;
    public String description;

    static {
        fieldNames.add("name");
        fieldNames.add("description");

        fieldNameToDescription.put("name", "Variable to re-define?");
        fieldNameToDescription.put("description", "Variable description?");
    }

    public RedefineVariablesRequest() {
        id = UUID.randomUUID();
    }

    public RedefineVariablesRequest(UUID missionId, VariableName name, String description) {
        this.missionId = missionId;
        this.name = name;
        this.description = description;
        variableNameToDescription = new Hashtable<VariableName, String>();
        variableNameToDescription.put(name, description);
        id = UUID.randomUUID();
    }

    public VariableName getVariableName() {
        return name;
    }

    public String getVariableDescription() {
        return description;
    }

    public Hashtable<VariableName, String> getVariableNameToDescription() {
        if (variableNameToDescription == null) {
            variableNameToDescription = new Hashtable<VariableName, String>();
            variableNameToDescription.put(name, description);
        }
        return variableNameToDescription;
    }

    public String toString() {
        return "RedefineVariablesRequest [" + name + ", " + description + "]";
    }
}
