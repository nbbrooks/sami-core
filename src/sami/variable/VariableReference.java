package sami.variable;

import java.io.Serializable;

/**
 *
 * @author nbb
 */
public class VariableReference implements Serializable {

    static final long serialVersionUID = 0L;
    public String variableName;

    public VariableReference(String variableName) {
        this.variableName = variableName;
    }

    public String toString() {
        return "VariableReference [" + variableName + "]";
    }
}
