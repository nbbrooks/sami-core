package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;
import sami.markupOption.NumberOption;

/**
 *
 * @author nbb
 */
public class NumberOfOptions extends Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // Description for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();
    // Fields
    public Number number;
    public Format format;
    public NumberOption numberOption;
    public static final int DEFAULT_NUM_OPTIONS = 1;

    public enum Number {

        SPECIFY
    };

    public enum Format {

        SEQUENTIAL, TABBED, STACKED
    };

    static {
        enumFieldNames.add("number");
        enumFieldNames.add("format");

        enumNameToDescription.put("number", "How many options to present?");
        enumNameToDescription.put("format", "How to present multiple options?");

        enumValueToFieldName.put(Number.SPECIFY, "numberOption");
        enumValueToFieldName.put(Format.SEQUENTIAL, null);
        enumValueToFieldName.put(Format.STACKED, null);
        enumValueToFieldName.put(Format.TABBED, null);
    }

    public NumberOfOptions() {
    }
}
