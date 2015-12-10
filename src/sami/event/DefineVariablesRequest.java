package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.variable.Variable;

public class DefineVariablesRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public ArrayList<Variable> variablesToDefine;

    static {
        fieldNames.add("variablesToDefine");

        fieldNameToDescription.put("variablesToDefine", "Variables to define?");
    }

    public DefineVariablesRequest() {
        id = UUID.randomUUID();
    }

    public DefineVariablesRequest(UUID missionId, ArrayList<Variable> variablesToDefine) {
        this.missionId = missionId;
        this.variablesToDefine = variablesToDefine;
        id = UUID.randomUUID();
    }

    public ArrayList<Variable> getVariablesToDefine() {
        return variablesToDefine;
    }

    public String toString() {
        return "DefineVariablesRequest [" + variablesToDefine + "]";
    }
}
