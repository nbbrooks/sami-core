package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author nbb
 */
public class RelevantInformation extends Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // Description for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();
    // Fields
    public Information information;
    public Visualization visualization;

    public enum Information {

        SPECIFY
    };

    public enum Visualization {

        HEATMAP, CONTOUR, THRESHOLD
    };

    static {
        enumFieldNames.add("information");
        enumFieldNames.add("visualization");

        enumNameToDescription.put("information", "What information to show?");
        enumNameToDescription.put("visualization", "How to visualize the information?");

        enumValueToFieldName.put(Information.SPECIFY, null);
        enumValueToFieldName.put(Visualization.CONTOUR, null);
        enumValueToFieldName.put(Visualization.HEATMAP, null);
        enumValueToFieldName.put(Visualization.THRESHOLD, null);
    }
}
