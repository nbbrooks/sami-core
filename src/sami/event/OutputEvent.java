package sami.event;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 *
 * @author pscerri
 */
public class OutputEvent extends Event {

    public HashMap<String, Field> getVariables() {
        return readVariableToField;
    }

    @Override
    public Object deepCopy() {
        OutputEvent copy = (OutputEvent) super.deepCopy();
        return copy;
    }
}
