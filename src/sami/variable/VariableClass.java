package sami.variable;

import java.io.Serializable;

/**
 *
 * @author nbb
 */
public class VariableClass implements Serializable {

    static final long serialVersionUID = 0L;
    public Class variableClass;

    public VariableClass(Class variableClass) {
        this.variableClass = variableClass;
    }

    public String toString() {
        return "VariableClass [" + variableClass.getSimpleName() + "]";
    }
}
