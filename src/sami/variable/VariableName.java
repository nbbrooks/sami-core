package sami.variable;

/**
 *
 * @author nbb
 */
public class VariableName implements java.io.Serializable {

    static final long serialVersionUID = 0L;
    public String variableName;

    public VariableName(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public boolean equals(Object o) {
        // Needed for setting saved value in JComboBox
        if (o instanceof VariableName) {
            boolean ret = variableName.equals(((VariableName) o).variableName);

            return ret;
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        return "VariableName [" + variableName + "]";
    }
}
