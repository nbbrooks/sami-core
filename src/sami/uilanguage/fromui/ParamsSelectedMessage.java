package sami.uilanguage.fromui;

import sami.event.ReflectedEventSpecification;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author pscerri
 */
public class ParamsSelectedMessage extends CreationDoneMessage {

    public ParamsSelectedMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId, eventSpecToFieldValues);
    }

    public String toString() {
        return "ParamsSelectedMessage [" + (eventSpecToFieldValues != null ? eventSpecToFieldValues.toString() : "null") + "]";
    }
}
