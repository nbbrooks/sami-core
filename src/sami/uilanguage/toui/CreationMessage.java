package sami.uilanguage.toui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;
import java.util.logging.Logger;
import sami.event.OperatorCreateOutputEvent;
import sami.event.ReflectedEventSpecification;
import sami.event.ReflectionHelper;
import sami.variable.Variable;
import sami.variable.VariableName;

/**
 * @author nbb
 */
public class CreationMessage extends ToUiMessage {

    private static final Logger LOGGER = Logger.getLogger(CreationMessage.class.getName());

    protected final Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions;
    protected final Hashtable<Field, String> fieldToDescriptions;
    protected final Hashtable<VariableName, String> variableNameToDescription;
    protected final ArrayList<Variable> variablesToDefine;
    
    public CreationMessage(UUID relevantOutputEventId, UUID missionId, int priority, Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions) {
        super(relevantOutputEventId, missionId, priority);
        this.eventSpecToFieldDescriptions = eventSpecToFieldDescriptions;
        this.fieldToDescriptions = null;
        this.variableNameToDescription = null;
        this.variablesToDefine = null;
    }

    public CreationMessage(UUID relevantOutputEventId, UUID missionId, int priority, Hashtable<VariableName, String> variableNameToDescription, int erasureThrowaway) {
        super(relevantOutputEventId, missionId, priority);
        this.eventSpecToFieldDescriptions = null;
        this.fieldToDescriptions = null;
        this.variableNameToDescription = variableNameToDescription;
        this.variablesToDefine = null;
    }

    public CreationMessage(UUID relevantOutputEventId, UUID missionId, int priority, ArrayList<Variable> variablesToDefine) {
        //@todo this isn't handled in getFromUiMessage or QueueItem
        super(relevantOutputEventId, missionId, priority);
        this.eventSpecToFieldDescriptions = null;
        this.fieldToDescriptions = null;
        this.variableNameToDescription = null;
        this.variablesToDefine = variablesToDefine;
    }

    public CreationMessage(UUID relevantOutputEventId, UUID missionId, int priority, OperatorCreateOutputEvent ooe) {
        super(relevantOutputEventId, missionId, priority);
        this.eventSpecToFieldDescriptions = null;
        this.variableNameToDescription = null;
        this.variablesToDefine = null;

        fieldToDescriptions = new Hashtable<Field, String>();
        try {
            Class matchingInputEventClass = ooe.getInputEventClass();
            HashMap<String, String> variableNameToDescription = (HashMap<String, String>) (matchingInputEventClass.getField("variableNameToDescription").get(null));
            for (String fieldName : variableNameToDescription.keySet()) {
                LOGGER.fine("\tField: " + fieldName);
                Field fieldToCreate = ReflectionHelper.getField(matchingInputEventClass, fieldName);
                fieldToDescriptions.put(fieldToCreate, variableNameToDescription.get(fieldName));
            }
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> getEventSpecToFieldDescriptions() {
        return eventSpecToFieldDescriptions;
    }

    public Hashtable<Field, String> getFieldToDescriptions() {
        return fieldToDescriptions;
    }

    public Hashtable<VariableName, String> getVariableNameToDescription() {
        return variableNameToDescription;
    }

    @Override
    public String toString() {
        return "CreationMessage [" + (eventSpecToFieldDescriptions != null ? eventSpecToFieldDescriptions.toString() : "null") + ", "
                + (variablesToDefine != null ? variableNameToDescription.toString() : "null")  + ", "
                + (variableNameToDescription != null ? variableNameToDescription.toString() : "null") + "]";
    }
}
