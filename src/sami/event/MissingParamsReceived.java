package sami.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class MissingParamsReceived extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Fields
    protected Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues;

    public MissingParamsReceived() {
        id = UUID.randomUUID();
    }

    public MissingParamsReceived(UUID relevantOutputEventId, UUID missionId, Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> eventSpecToFieldValues) {
        this.relevantOutputEventId = relevantOutputEventId;
        this.missionId = missionId;
        this.eventSpecToFieldValues = eventSpecToFieldValues;
        id = UUID.randomUUID();
    }

    public Hashtable<ReflectedEventSpecification, Hashtable<Field, Object>> getEventSpecToFieldValues() {
        return eventSpecToFieldValues;
    }

    public String toString() {
        return "MissingParamsReceived: " + (eventSpecToFieldValues != null ? eventSpecToFieldValues.toString() : "null");
    }
}
