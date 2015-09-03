package sami.markup;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 *
 * @author nbb
 */
public class MarkupOption implements java.io.Serializable {

    // Forgot to check this in initially with a serialVersionUID
    // Uncomment this line when committing a change affecting serialization
    //static final long serialVersionUID = 1L;

    private HashMap<String, Field> variableNameToField = new HashMap<String, Field>();
    private HashMap<Field, String> fieldToVariableName = new HashMap<Field, String>();

    public HashMap<String, Field> getVariables() {
        return variableNameToField;
    }

    public void addVariable(String variableName, Field field) {
        if (variableNameToField == null) {
            variableNameToField = new HashMap<String, Field>();
        }
        if (fieldToVariableName == null) {
            fieldToVariableName = new HashMap<Field, String>();
        }
        variableNameToField.put(variableName, field);
        fieldToVariableName.put(field, variableName);
    }

    public String getVariableForField(Field field) {
        if (fieldToVariableName == null) {
            fieldToVariableName = new HashMap<Field, String>();
            return null;
        }
        return fieldToVariableName.get(field);
    }
}
