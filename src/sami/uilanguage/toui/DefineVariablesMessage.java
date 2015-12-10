package sami.uilanguage.toui;

import java.util.ArrayList;
import java.util.UUID;
import sami.variable.Variable;

/**
 *
 * @author nbb
 */
public class DefineVariablesMessage extends CreationMessage {

    public DefineVariablesMessage(UUID relevantOutputEventId, UUID missionId, int priority, ArrayList<Variable> variablesToDefine) {
        super(relevantOutputEventId, missionId, priority, variablesToDefine);
    }

    public String toString() {
        return "DefineVariablesMessage [" + (variablesToDefine != null ? variablesToDefine.toString() : "null") + "]";
    }
}
