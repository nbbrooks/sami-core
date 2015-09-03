package sami.uilanguage.fromui;

import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class VariablesDefinedMessage extends CreationDoneMessage {

    public VariablesDefinedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Hashtable<String, Object> variableToValue) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId, variableToValue, 0, 0);
    }

    public String toString() {
        return "VariablesDefinedMessage [" + (variableToValue != null ? variableToValue.toString() : "null") + "]";
    }
}
