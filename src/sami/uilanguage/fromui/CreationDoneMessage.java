package sami.uilanguage.fromui;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.ReflectedEventSpecification;

/**
 *
 * @author nbb
 */
public abstract class CreationDoneMessage extends FromUiMessage {

    protected final Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues;
    protected final Hashtable<String, Object> variableToValue;

    public CreationDoneMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.eventSpecToFieldValues = eventSpecToFieldValues;
        this.variableToValue = null;
    }

    public CreationDoneMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues, Hashtable<String, Object> variableToValue) {
        // eventSpecToFieldValues is intended to be null, but is needed for constructor definition erasure reasons
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.eventSpecToFieldValues = eventSpecToFieldValues;
        this.variableToValue = variableToValue;
    }

    public Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> getEventSpecToFieldValues() {
        return eventSpecToFieldValues;
    }

    public Hashtable<String, Object> getVariableToValue() {
        return variableToValue;
    }

    public String toString() {
        return "CreationDoneMessage [" + (eventSpecToFieldValues != null ? eventSpecToFieldValues.toString() : "null") + ", " + (variableToValue != null ? variableToValue.toString() : "null") + "]";
    }
}
