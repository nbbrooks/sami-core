package sami.uilanguage.fromui;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.ReflectedEventSpecification;

/**
 *
 * @author nbb
 */
public class CreationDoneMessage extends FromUiMessage {

    protected final Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues;
    protected final Hashtable<Field, Object> fieldToValues;
    protected final Hashtable<String, Object> variableToValue;

    public CreationDoneMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.eventSpecToFieldValues = eventSpecToFieldValues;
        this.fieldToValues = null;
        this.variableToValue = null;
    }

    /**
     *
     * @param relevantToUiMessageId
     * @param relevantOutputEventId
     * @param missionId
     * @param fieldToValues
     * @param typeErasure not used, needed due to type erasure
     */
    public CreationDoneMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Hashtable<Field, Object> fieldToValues, int typeErasure) {
        // eventSpecToFieldValues is intended to be null, but is needed for constructor definition erasure reasons
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.eventSpecToFieldValues = null;
        this.fieldToValues = fieldToValues;
        this.variableToValue = null;
    }

    /**
     *
     * @param relevantToUiMessageId
     * @param relevantOutputEventId
     * @param missionId
     * @param variableToValue
     * @param typeErasure1 not used, needed due to type erasure
     * @param typeErasure2 not used, needed due to type erasure
     */
    public CreationDoneMessage(UUID relevantToUiMessageId, UUID relevantOutputEventId, UUID missionId, Hashtable<String, Object> variableToValue, int typeErasure1, int typeErasure2) {
        super(relevantToUiMessageId, relevantOutputEventId, missionId);
        this.eventSpecToFieldValues = null;
        this.fieldToValues = null;
        this.variableToValue = variableToValue;
    }

    public Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> getEventSpecToFieldValues() {
        return eventSpecToFieldValues;
    }

    public Hashtable<Field, Object> getFieldToValues() {
        return fieldToValues;
    }

    public Hashtable<String, Object> getVariableToValue() {
        return variableToValue;
    }

    @Override
    public String toString() {
        return "CreationDoneMessage [" + (eventSpecToFieldValues != null ? eventSpecToFieldValues.toString() : "null") + ", " + (fieldToValues != null ? fieldToValues.toString() : "null") + ", " + (variableToValue != null ? variableToValue.toString() : "null") + "]";
    }
}
