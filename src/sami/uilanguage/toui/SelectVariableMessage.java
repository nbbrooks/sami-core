package sami.uilanguage.toui;

import java.util.List;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class SelectVariableMessage extends SelectionMessage {

    public SelectVariableMessage(UUID relevantOutputEventId, UUID missionId, int priority, boolean allowMultiple, List<String> optionsList) {
        super(relevantOutputEventId, missionId, priority, allowMultiple, false, true, optionsList);
    }

    public String toString() {
        return "SelectVariableMessage [" + (optionsList != null ? optionsList.toString() : "null") + "]";
    }
}
