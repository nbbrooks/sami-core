package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;
import sami.markupOption.TextOption;

/**
 *
 * @author nbb
 */
public class Description extends Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // Description for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();
    // Fields
    public TextSourceEnum textSourceEnum;
//    public ShowPlanNameEnum showPlanNameEnum;
//    public ShowVertexNameNum showVertexNameEnum;
//    public ShowEventNameEnum showEventNameEnum;
    public TextOption textOption;

    public enum TextSourceEnum {

        SPECIFY
    };

    public enum ShowPlanNameEnum {

        YES, NO
    };

    public enum ShowVertexNameNum {

        YES, NO
    };

    public enum ShowEventNameEnum {

        YES, NO
    };

    static {
        enumFieldNames.add("textSourceEnum");
//        enumFieldNames.add("showPlanNameEnum");
//        enumFieldNames.add("showVertexNameEnum");
//        enumFieldNames.add("showEventNameEnum");

        enumNameToDescription.put("textSourceEnum", "Source of the text to be displayed?");
//        enumNameToDescription.put("showPlanNameEnum", "Display plan name?");
//        enumNameToDescription.put("showVertexNameEnum", "Display vertex name?");
//        enumNameToDescription.put("showEventNameEnum", "Display event name?");

        enumValueToFieldName.put(TextSourceEnum.SPECIFY, "textOption");
//        enumValueToFieldName.put(ShowPlanNameEnum.YES, null);
//        enumValueToFieldName.put(ShowPlanNameEnum.NO, null);
//        enumValueToFieldName.put(ShowVertexNameNum.YES, null);
//        enumValueToFieldName.put(ShowVertexNameNum.NO, null);
//        enumValueToFieldName.put(ShowEventNameEnum.YES, null);
//        enumValueToFieldName.put(ShowEventNameEnum.NO, null);
    }

    public Description() {
    }
}
