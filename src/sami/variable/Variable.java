package sami.variable;

/**
 * Contains the information needed for a operator to define a variable at
 * run-time.
 *
 * @author nbb
 */
public class Variable {

    //@todo add in Array support (with a defined array length)
    public enum ClassQuantity {

        SINGLE, ARRAY_LIST
    };

    // Variable to write the value to?
    //@todo add in choice for local v global scope?
//    public enum Scope {
//
//        LOCAL, GLOBAL
//    };
    public String variableName;
    // Description of the variable so the run-time operator knows what they are defining
    public String variableDescription;
    // The number of instances of the class we want the operator to define for this variable
    public ClassQuantity classQuantity;
    // The class we want the operator to define for this variable
    public VariableClass variableClass;

    public Variable() {

    }

    public Variable(String variableName, String variableDescription, ClassQuantity classQuantity, VariableClass variableClass) {
        this.variableName = variableName;
        this.variableDescription = variableDescription;
        this.classQuantity = classQuantity;
        this.variableClass = variableClass;
    }

    public String toString() {
        return "Variable [" + variableName + ", " + variableDescription + ", " + classQuantity + ", " + variableClass + "]";
    }
}