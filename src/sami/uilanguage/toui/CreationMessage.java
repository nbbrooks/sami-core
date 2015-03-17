package sami.uilanguage.toui;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.UUID;
import sami.event.ReflectedEventSpecification;
import sami.variable.VariableName;

/**
 * @author nbb
 */
public abstract class CreationMessage extends ToUiMessage {

    protected final Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions;
    protected final Hashtable<VariableName, String> variableNameToDescription;

    public CreationMessage(UUID relevantOutputEventId, UUID missionId, int priority, Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions) {
        super(relevantOutputEventId, missionId, priority);
        this.eventSpecToFieldDescriptions = eventSpecToFieldDescriptions;
        this.variableNameToDescription = null;
    }

    public CreationMessage(UUID relevantOutputEventId, UUID missionId, int priority, Hashtable<VariableName, String> variableNameToDescription, int dump) {
        super(relevantOutputEventId, missionId, priority);
        this.eventSpecToFieldDescriptions = null;
        this.variableNameToDescription = variableNameToDescription;
    }

    public Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> getEventSpecToFieldDescriptions() {
        return eventSpecToFieldDescriptions;
    }

    public Hashtable<VariableName, String> getVariableNameToDescription() {
        return variableNameToDescription;
    }

    public String toString() {
        return "CreationMessage [" + (eventSpecToFieldDescriptions != null ? eventSpecToFieldDescriptions.toString() : "null") + ", "
                + (variableNameToDescription != null ? variableNameToDescription.toString() : "null") + "]";
    }
}
