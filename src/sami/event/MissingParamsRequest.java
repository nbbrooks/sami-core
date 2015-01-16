package sami.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

public class MissingParamsRequest extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    protected final Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions;

    public MissingParamsRequest(UUID missionId, Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> eventSpecToFieldDescriptions) {
        this.missionId = missionId;
        this.eventSpecToFieldDescriptions = eventSpecToFieldDescriptions;
        id = UUID.randomUUID();
    }

    public Hashtable<ReflectedEventSpecification, Hashtable<Field, String>> getEventToFieldDescriptions() {
        return eventSpecToFieldDescriptions;
    }

    public String toString() {
        return "MissingParamsRequest [" + eventSpecToFieldDescriptions + "]";
    }
}
