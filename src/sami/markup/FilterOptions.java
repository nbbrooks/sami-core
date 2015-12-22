package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;
import sami.markupOption.TextOption;

/**
 *
 * @author nbb
 */
public class FilterOptions extends Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // FilterOptions for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();
    // Fields
    public FilterMethodEnum filterMethodEnum;
    public IncludeMatchEnum includeMatchEnum;
    public TextOption textOption;

    public enum FilterMethodEnum {

        TEXT
    };

    public enum IncludeMatchEnum {

        INCLUDE, EXCLUDE
    };

    static {
        enumFieldNames.add("filterMethodEnum");
        enumFieldNames.add("includeMatchEnum");

        enumNameToDescription.put("filterMethodEnum", "How to filter?");
        enumNameToDescription.put("includeMatchEnum", "Include or exlude items matching filter?");

        enumValueToFieldName.put(FilterMethodEnum.TEXT, "textOption");
        enumValueToFieldName.put(IncludeMatchEnum.INCLUDE, null);
        enumValueToFieldName.put(IncludeMatchEnum.EXCLUDE, null);
    }

    public FilterOptions() {
    }
}
