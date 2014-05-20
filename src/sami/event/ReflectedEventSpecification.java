package sami.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.SerializableHelper;
import static sami.event.Event.NONE;
import sami.markup.Markup;
import sami.markup.ReflectedMarkupSpecification;

/**
 * Stores a reflected InputEvent or OutputEvent classname and any parameters for
 * the event
 *
 * This is one of the key classes for mapping between DREAMM and SAMI
 *
 * The complexity is due to variables being passed back and forth and
 * instantiated at runtime
 *
 * @author pscerri
 */
public class ReflectedEventSpecification implements java.io.Serializable {

    private static final Logger LOGGER = Logger.getLogger(ReflectedEventSpecification.class.getName());
    static final long serialVersionUID = 0L;
    //@todo need to decide if we should have to handle non-serializable classes or not...making the below unneeded - right now we assume everything is serializable
    // Non-serializable lookup from field name to object representing its defined/undefined variable name or value
    transient HashMap<String, Object> fieldNameToTransDefinition = new HashMap<String, Object>();
    // Event's reflected markup specs
    protected ArrayList<ReflectedMarkupSpecification> markupSpecs = new ArrayList<ReflectedMarkupSpecification>();
    // Serializable version of fieldNameToObjectInst using HashMaps and Strings to represent object
    protected HashMap<String, Object> fieldNameToSerialDefinition = new HashMap<String, Object>();
    // For each field, whether or not to allow the user to edit the values at run-time
    protected HashMap<String, Boolean> fieldNameToEditable = new HashMap<String, Boolean>();
    // Event's class name
    protected final String className;

    public ReflectedEventSpecification(String className) {
        this.className = className;
    }

    public void addVariablePrefix(String prefix) {
        //@todo what about sub-field hashmaps?
        HashMap<String, Object> newFieldNameToObject = new HashMap<String, Object>();
        for (String fieldName : fieldNameToTransDefinition.keySet()) {
            Object object = fieldNameToTransDefinition.get(fieldName);
            LOGGER.log(Level.FINEST, "Have <field, object>: " + fieldName + ", " + object);
            if (object != null && object instanceof String && ((String) object).startsWith("@") && !((String) object).equalsIgnoreCase(NONE)) {
                String newVariable = "@" + prefix + "." + ((String) object).substring(1);
                newFieldNameToObject.put(fieldName, newVariable);
            } else {
                newFieldNameToObject.put(fieldName, object);
            }
        }
        fieldNameToTransDefinition = newFieldNameToObject;
    }

    public ArrayList<ReflectedMarkupSpecification> getMarkupSpecs() {
        return markupSpecs;
    }

    public void setMarkupSpecs(ArrayList<ReflectedMarkupSpecification> markupSpecs) {
        this.markupSpecs = markupSpecs;
    }

    public Object getFieldDefinition(String fieldName) {
        return fieldNameToTransDefinition.get(fieldName);
    }

    public HashMap<String, Object> getFieldDefinitions() {
        return fieldNameToTransDefinition;
    }

    public HashMap<String, Boolean> getEditableFields() {
        return fieldNameToEditable;
    }

    public void setFieldDefinitions(HashMap<String, Object> fieldNameToObject) {
        fieldNameToTransDefinition = fieldNameToObject;
    }

    public void setEditableFields(HashMap<String, Boolean> fieldNameToEditable) {
        this.fieldNameToEditable = fieldNameToEditable;
    }

    public void addFieldDefinition(String fieldName, Object fieldValue) {
        fieldNameToTransDefinition.put(fieldName, fieldValue);
    }

    public String getClassName() {
        return className;
    }

    /**
     * Returns true if any fields do not have a definition
     *
     * @param atPlace
     * @return
     */
    public boolean hasMissingParams(boolean atPlace) {
        //@todo this is ugly
        try {
            Class c = Class.forName(className);
            Event event = (Event) c.newInstance();
            LOGGER.log(Level.FINE, "Event " + event.getClass().getSimpleName() + ", event.getFillAtPlace() = " + event.getFillAtPlace());
            if (event.getFillAtPlace() && !atPlace) {
                LOGGER.log(Level.FINE, "Tried to check for missing params for event " + event.getClass().getSimpleName() + " at plan loading, but params are to be filled at place, returning false");
                return false;
            } else {
                LOGGER.log(Level.FINE, "Checking for missing params for event " + event.getClass().getSimpleName());
                for (String fieldName : fieldNameToTransDefinition.keySet()) {
                    if (fieldNameToTransDefinition.get(fieldName) == null) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate event: " + className + " due to " + ex, this);
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Returns true if any fields do not have a definition or are editable
     *
     * @param atPlace
     * @return
     */
    public boolean hasEditableParams(boolean atPlace) {
        //@todo this is ugly
        try {
            Class c = Class.forName(className);
            Event event = (Event) c.newInstance();
            LOGGER.log(Level.FINE, "Event " + event.getClass().getSimpleName() + ", event.getFillAtPlace() = " + event.getFillAtPlace());
            if (event.getFillAtPlace() && !atPlace) {
                LOGGER.log(Level.FINE, "Tried to check for editable params for event " + event.getClass().getSimpleName() + " at plan loading, but params are to be filled at place, returning false");
                return false;
            } else {
                LOGGER.log(Level.FINE, "Checking for editable params for event " + event.getClass().getSimpleName());
                for (String fieldName : fieldNameToTransDefinition.keySet()) {
                    if (fieldNameToTransDefinition.get(fieldName) == null
                            || (fieldNameToEditable.containsKey(fieldName) && fieldNameToEditable.get(fieldName))) {
                        // If a param has no defintion or is editable, it needs to be specified/should be editable
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.severe("Failed to instantiate event: " + className);
            ex.printStackTrace();
        }
        return false;
    }

    public String toString() {
        return className.substring(className.lastIndexOf(".") + 1);
    }

    /**
     * Return an instance of the stored Event class with values set to those
     * stored in fieldNameToTransObject
     *
     * @return event
     */
    public Event instantiate() {
        LOGGER.log(Level.FINE, "Instantiate event called for " + className, this);
        Event event = null;
        try {
            Class c = Class.forName(className);
            event = (Event) c.newInstance();

            if (event != null) {
                instantiateEventVariables(event, fieldNameToTransDefinition);
                instantiateMarkups(event, markupSpecs);
            } else {
                LOGGER.log(Level.SEVERE, "Creation of instance failed for " + className, this);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate event: " + className + " due to " + ex, this);
            ex.printStackTrace();
        }

        return event;
    }

    private void instantiateEventVariables(Event event, HashMap<String, Object> fieldNameToObject) {
        for (Field field : event.getClass().getDeclaredFields()) {
            Object definition = fieldNameToObject.get(field.getName());
            if (definition != null) {
                try {
                    // Earlier we had to replace primitive fields with their wrapper object, undo that here
                    if (definition.getClass().equals(String.class) && ((String) definition).startsWith("@")) {
                        // This is a variable name
                        if (event instanceof InputEvent) {
                            ((InputEvent) event).addVariable(field.getName(), (String) definition);
                        }
                        if (event instanceof OutputEvent) {
                            ((OutputEvent) event).addVariable((String) definition, field);
                        }
                    } else if (field.getType().equals(double.class) && definition.getClass().equals(Double.class)) {
                        field.setDouble(event, ((Double) definition).doubleValue());
                    } else if (field.getType().equals(float.class) && definition.getClass().equals(Float.class)) {
                        field.setFloat(event, ((Float) definition).floatValue());
                    } else if (field.getType().equals(int.class) && definition.getClass().equals(Integer.class)) {
                        field.setInt(event, ((Integer) definition).intValue());
                    } else if (field.getType().equals(long.class) && definition.getClass().equals(Long.class)) {
                        field.setLong(event, ((Long) definition).longValue());
                    } else {
                        field.set(event, definition);
                        if (definition != null && field.get(event) == null) {
                            LOGGER.log(Level.SEVERE, "Instantiation of field " + field.getName() + " on Event " + event.toString() + " failed!");
                        }
                    }
                } catch (IllegalAccessException iae) {
                    iae.printStackTrace();
                }
            }
        }
    }

    private void instantiateMarkups(Event event, ArrayList<ReflectedMarkupSpecification> markupSpecs) {
        ArrayList<Markup> markups = new ArrayList<Markup>();
        for (ReflectedMarkupSpecification markupSpec : markupSpecs) {
            markups.add(markupSpec.instantiate());
        }
        event.setMarkups(markups);
    }

    public ReflectedEventSpecification copy() {
        ReflectedEventSpecification copy = new ReflectedEventSpecification(className);
        // Need to set variableHolderField or instanceParams?
        if (fieldNameToTransDefinition != null) {
            copy.fieldNameToTransDefinition = new HashMap<String, Object>();
            for (String key : fieldNameToTransDefinition.keySet()) {
                copy.fieldNameToTransDefinition.put(key, fieldNameToTransDefinition.get(key));
            }
        }
        if (fieldNameToEditable != null) {
            copy.fieldNameToEditable = new HashMap<String, Boolean>();
            for (String key : fieldNameToEditable.keySet()) {
                copy.fieldNameToEditable.put(key, fieldNameToEditable.get(key));
            }
        }
        if (markupSpecs != null) {
            copy.markupSpecs = new ArrayList<ReflectedMarkupSpecification>();
            for (ReflectedMarkupSpecification markupSpec : markupSpecs) {
                copy.markupSpecs.add(markupSpec.copy());
            }
        }
        return copy;
    }

    /**
     * Override the default serialization This is required because Fields are
     * not serializable
     *
     * @param os
     */
    private void writeObject(ObjectOutputStream os) {
        try {
            fieldNameToSerialDefinition = SerializableHelper.transientToSerial(fieldNameToTransDefinition);
            os.defaultWriteObject();
        } catch (IOException ex) {
            Logger.getLogger(ReflectedEventSpecification.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            fieldNameToTransDefinition = SerializableHelper.serialToTransient(className, fieldNameToSerialDefinition);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
