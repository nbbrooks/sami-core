package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class ReturnValue extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields
    public String value;

    static {
        fieldNames.add("value");

        fieldNameToDescription.put("value", "Return value?");
    }

    public ReturnValue() {
        id = UUID.randomUUID();
    }

    public String getReturnValue() {
        return value;
    }

    public String toString() {
        return "ReturnValue [" + value + "]";
    }
}
