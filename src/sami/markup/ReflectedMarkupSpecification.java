package sami.markup;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.SerializableHelper;
import sami.event.ReflectedEventSpecification;

/**
 *
 * @author nbb
 */
public class ReflectedMarkupSpecification implements java.io.Serializable {

    private final static Logger LOGGER = Logger.getLogger(ReflectedMarkupSpecification.class.getName());
    static final long serialVersionUID = 1L;
    //@todo need to decide if we should have to handle non-serializable classes or not...making the below unneeded - right now we assume everything is serializable
    // Non-serializable lookup from field name to object representing its defined/undefined variable name or value
    transient HashMap<String, Object> fieldNameToTransDefinition = new HashMap<String, Object>();
    // Serializable version of fieldNameToObjectInst using HashMaps and Strings to represent object
    protected HashMap<String, Object> fieldNameToSerialDefinition = new HashMap<String, Object>();
    // Markup's class name
    protected final String className;

    public ReflectedMarkupSpecification(String className) {
        this.className = className;
    }

    public HashMap<String, Object> getFieldDefinitions() {
        return fieldNameToTransDefinition;
    }

    public void setFieldDefinitions(HashMap<String, Object> fieldNameToObject) {
        fieldNameToTransDefinition = fieldNameToObject;
    }

    public void addFieldDefinition(String fieldName, Object fieldValue) {
        fieldNameToTransDefinition.put(fieldName, fieldValue);
    }

    public String getClassName() {
        return className;
    }

    public Markup instantiate() {
        LOGGER.log(Level.FINE, "Instantiate event called for " + className, this);
        Markup markup = null;
        try {
            Class c = Class.forName(className);
            markup = (Markup) c.newInstance();

            if (markup != null) {
                instantiateMarkupVariables(markup, fieldNameToTransDefinition);
            } else {
                LOGGER.log(Level.SEVERE, "Creation of instance failed for " + className, this);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate event: " + className + " due to " + ex, this);
            ex.printStackTrace();
        }

        return markup;
    }

    private void instantiateMarkupVariables(Markup markup, HashMap<String, Object> fieldNameToObject) {
        for (Field field : markup.getClass().getDeclaredFields()) {
            Object definition = fieldNameToObject.get(field.getName());
            if (definition != null) {
                try {
                    // Earlier we had to replace primitive fields with their wrapper object, undo that here
                    if (definition.getClass().equals(String.class) && ((String) definition).startsWith("@")) {
                        // This is a variable name
                        markup.addVariable(field.getName(), (String) definition);
                    } else if (field.getType().equals(double.class) && definition.getClass().equals(Double.class)) {
                        field.setDouble(markup, ((Double) definition).doubleValue());
                    } else if (field.getType().equals(float.class) && definition.getClass().equals(Float.class)) {
                        field.setFloat(markup, ((Float) definition).floatValue());
                    } else if (field.getType().equals(int.class) && definition.getClass().equals(Integer.class)) {
                        field.setInt(markup, ((Integer) definition).intValue());
                    } else if (field.getType().equals(long.class) && definition.getClass().equals(Long.class)) {
                        field.setLong(markup, ((Long) definition).longValue());
                    } else {
                        field.set(markup, definition);
                        if (definition != null && field.get(markup) == null) {
                            LOGGER.log(Level.SEVERE, "Instantiation of field " + field.getName() + " on Markup " + markup.toString() + " failed!");
                        }
                    }
                } catch (IllegalAccessException iae) {
                    iae.printStackTrace();
                }
            }
        }
    }

    public ReflectedMarkupSpecification copy() {
        ReflectedMarkupSpecification copy = new ReflectedMarkupSpecification(className);
        copy.fieldNameToTransDefinition = new HashMap<String, Object>();
        for (String key : fieldNameToTransDefinition.keySet()) {
            copy.fieldNameToTransDefinition.put(key, fieldNameToTransDefinition.get(key));
        }
        return copy;
    }

    @Override
    public String toString() {
        return className.substring(className.lastIndexOf(".") + 1);
    }

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
