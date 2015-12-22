package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author nbb
 */
public class DefaultSelection extends Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // Description for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();
    // Fields
    public Selection selection;

    public enum Selection {

        ALL, NONE
    };

    static {
        enumFieldNames.add("selection");

        enumNameToDescription.put("selection", "Default selection??");

        enumValueToFieldName.put(Selection.ALL, null);
        enumValueToFieldName.put(Selection.NONE, null);
    }

    public DefaultSelection() {
    }
}
