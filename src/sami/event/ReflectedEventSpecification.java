package sami.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    static final long serialVersionUID = 1L;
    // For each field, an instantiated object value
    private HashMap<String, Object> fieldNameToValue = new HashMap<String, Object>();
    // For each field, variable name field's value should be read from at run-time
    private HashMap<String, String> fieldNameToReadVariable = new HashMap<String, String>();
    // For each field, variable name field's value should be written to at run-time (input events only)
    private HashMap<String, String> fieldNameToWriteVariable = new HashMap<String, String>();
    // For each field, whether or not to allow the user to edit the values at run-time
    private HashMap<String, Boolean> fieldNameToEditable = new HashMap<String, Boolean>();
    // Event's reflected markup specs
    private ArrayList<ReflectedMarkupSpecification> markupSpecs = new ArrayList<ReflectedMarkupSpecification>();
    // Event's class name
    private final String className;

    public ReflectedEventSpecification(String className) {
        this.className = className;
    }

    public void addVariablePrefix(String prefix, HashMap<String, Object> globalVariables) {
        // Read variables
        HashMap<String, String> newFieldNameToReadVariable = new HashMap<String, String>();
        for (String fieldName : fieldNameToReadVariable.keySet()) {
            String readVariable = fieldNameToReadVariable.get(fieldName);
            if (!readVariable.equalsIgnoreCase(NONE)) {
                if (globalVariables.containsKey(readVariable)) {
                    newFieldNameToReadVariable.put(fieldName, readVariable);
                } else {
                    String newVariable = "@" + prefix + "." + readVariable.substring(1);
                    newFieldNameToReadVariable.put(fieldName, newVariable);
                }
            } else {
                newFieldNameToReadVariable.put(fieldName, NONE);
            }
        }
        fieldNameToReadVariable = newFieldNameToReadVariable;

        // Write variables (input events)
        HashMap<String, String> newFieldNameToWriteVariable = new HashMap<String, String>();
        for (String fieldName : fieldNameToWriteVariable.keySet()) {
            String writeVariable = fieldNameToWriteVariable.get(fieldName);
            if (!writeVariable.equalsIgnoreCase(NONE)) {
                if (globalVariables.containsKey(writeVariable)) {
                    newFieldNameToWriteVariable.put(fieldName, writeVariable);
                } else {
                    String newVariable = "@" + prefix + "." + writeVariable.substring(1);
                    newFieldNameToWriteVariable.put(fieldName, newVariable);
                }
            } else {
                newFieldNameToWriteVariable.put(fieldName, NONE);
            }
        }
        fieldNameToWriteVariable = newFieldNameToWriteVariable;
    }

    public ArrayList<ReflectedMarkupSpecification> getMarkupSpecs() {
        return markupSpecs;
    }

    public void setMarkupSpecs(ArrayList<ReflectedMarkupSpecification> markupSpecs) {
        this.markupSpecs = markupSpecs;
    }

    public HashMap<String, Object> getFieldValues() {
        return fieldNameToValue;
    }

    public void setFieldValues(HashMap<String, Object> fieldNameToValue) {
        this.fieldNameToValue = fieldNameToValue;
    }

    public void addFieldValue(String fieldName, Object value) {
        fieldNameToValue.put(fieldName, value);
    }

    public HashMap<String, String> getReadVariables() {
        return fieldNameToReadVariable;
    }

    public void setReadVariables(HashMap<String, String> fieldNameToReadVariable) {
        this.fieldNameToReadVariable = fieldNameToReadVariable;
    }

    public HashMap<String, String> getWriteVariables() {
        return fieldNameToWriteVariable;
    }

    public void setWriteVariables(HashMap<String, String> fieldNameToWriteVariable) {
        this.fieldNameToWriteVariable = fieldNameToWriteVariable;
    }

    public HashMap<String, Boolean> getEditableFields() {
        return fieldNameToEditable;
    }

    public void setEditableFields(HashMap<String, Boolean> fieldNameToEditable) {
        this.fieldNameToEditable = fieldNameToEditable;
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
        try {
            Class eventClass = Class.forName(className);
            Event event = (Event) eventClass.newInstance();
            if (event.getFillAtPlace() && !atPlace) {
                LOGGER.log(Level.FINE, "Tried to check for missing params for event " + eventClass.getSimpleName() + " at plan loading, but params are to be filled at place, returning false");
                return false;
            } else {
                ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
                for (String fieldName : fieldNames) {
                    if ((!fieldNameToValue.containsKey(fieldName) || fieldNameToValue.get(fieldName) == null)
                            && (!fieldNameToReadVariable.containsKey(fieldName) || fieldNameToReadVariable.get(fieldName).equals(NONE))) {
                        return true;
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
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
        try {
            Class eventClass = Class.forName(className);
            Event event = (Event) eventClass.newInstance();
            if (event.getFillAtPlace() && !atPlace) {
                LOGGER.log(Level.FINE, "Tried to check for editable params for event " + event.getClass().getSimpleName() + " at plan loading, but params are to be filled at place, returning false");
                return false;
            } else {
                ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
                for (String fieldName : fieldNames) {
                    if (fieldNameToEditable.containsKey(fieldName) && fieldNameToEditable.get(fieldName).booleanValue()) {
                        return true;
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
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
            Class eventClass = Class.forName(className);
            event = (Event) eventClass.newInstance();
            instantiateEventVariables(event, eventClass);
            instantiateMarkups(event);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        }
        return event;
    }

    private void instantiateEventVariables(Event event, Class eventClass) {
        try {
            // Add read variables and set field values with definitions
            ArrayList<String> fieldNames = (ArrayList<String>) (eventClass.getField("fieldNames").get(null));
            for (String fieldName : fieldNames) {
                if (fieldNameToReadVariable.containsKey(fieldName)) {
                    // Add read variable
                    event.addReadVariable(fieldNameToReadVariable.get(fieldName), ReflectionHelper.getField(eventClass, fieldName));
                } else if (fieldNameToValue.containsKey(fieldName)) {
                    Object value = fieldNameToValue.get(fieldName);
                    Field field = ReflectionHelper.getField(eventClass, fieldName);
                    if (field.getType().equals(double.class) && value.getClass().equals(Double.class)) {
                        field.setDouble(event, ((Double) value).doubleValue());
                    } else if (field.getType().equals(float.class) && value.getClass().equals(Float.class)) {
                        field.setFloat(event, ((Float) value).floatValue());
                    } else if (field.getType().equals(int.class) && value.getClass().equals(Integer.class)) {
                        field.setInt(event, ((Integer) value).intValue());
                    } else if (field.getType().equals(long.class) && value.getClass().equals(Long.class)) {
                        field.setLong(event, ((Long) value).longValue());
                    } else {
                        field.set(event, value);
                        if (value != null && field.get(event) == null) {
                            LOGGER.log(Level.SEVERE, "Instantiation of field " + field.getName() + " on Event " + event.toString() + " failed!");
                        }
                    }
                }
            }

            // Add write variables  
            if (InputEvent.class.isAssignableFrom(eventClass)) {
                ArrayList<String> variableNames = (ArrayList<String>) (eventClass.getField("variableNames").get(null));
                for (String variableName : variableNames) {
                    if (fieldNameToWriteVariable.containsKey(variableName)) {
                        ((InputEvent) event).addWriteVariable(variableName, fieldNameToWriteVariable.get(variableName));
                    }
                }
            }
        } catch (NoSuchFieldException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    private void instantiateMarkups(Event event) {
        ArrayList<Markup> markups = new ArrayList<Markup>();
        for (ReflectedMarkupSpecification markupSpec : markupSpecs) {
            markups.add(markupSpec.instantiate());
        }
        event.setMarkups(markups);
    }
}
