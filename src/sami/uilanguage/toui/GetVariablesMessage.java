package sami.uilanguage.toui;

import java.util.Hashtable;
import java.util.UUID;
import sami.variable.VariableName;

/**
 *
 * @author nbb
 */
public class GetVariablesMessage extends CreationMessage {

    public GetVariablesMessage(UUID relevantOutputEventId, UUID missionId, int priority, Hashtable<VariableName, String> variableNameToDescription) {
        super(relevantOutputEventId, missionId, priority, variableNameToDescription, 0);
    }

    public String toString() {
        return "GetVariablesMessage [" + (variableNameToDescription != null ? variableNameToDescription.toString() : "null") + "]";
    }
}
