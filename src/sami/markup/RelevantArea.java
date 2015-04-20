package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;
import sami.markupOption.AreaOption;
import sami.markupOption.PointOption;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class RelevantArea extends Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // Description for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();
    // Fields
    public AreaSelection areaSelection;
    public ViewModification viewModification;
    public MapType mapType;
    public AreaOption areaOption;
    public PointOption pointOption;
    // Fields not shown to developer
    protected ArrayList<ProxyInt> relevantProxies;

    public enum AreaSelection {

        AREA, ALL_PROXIES, RELEVANT_PROXIES, POINT
    };

    public enum ViewModification {

        EXPAND, REDUCE
    };

    public enum MapType {

        SATELLITE, POLITICAL
    };

    static {
        enumFieldNames.add("areaSelection");
        enumFieldNames.add("viewModification");
        enumFieldNames.add("mapType");

        enumNameToDescription.put("areaSelection", "What area to show?");
        enumNameToDescription.put("viewModification", "How to modify existing (if applicable) map view?");
        enumNameToDescription.put("mapType", "What map type to use?");

        enumValueToFieldName.put(AreaSelection.AREA, "areaOption");
        enumValueToFieldName.put(AreaSelection.ALL_PROXIES, null);
        enumValueToFieldName.put(AreaSelection.POINT, "pointOption");
        enumValueToFieldName.put(AreaSelection.RELEVANT_PROXIES, null);
        enumValueToFieldName.put(ViewModification.EXPAND, null);
        enumValueToFieldName.put(ViewModification.REDUCE, null);
        enumValueToFieldName.put(MapType.POLITICAL, null);
        enumValueToFieldName.put(MapType.SATELLITE, null);
    }

    public RelevantArea() {
    }

    @Override
    public RelevantArea copy() {
        RelevantArea copy = new RelevantArea();
        if (fieldNameToVariableName != null) {
            copy.fieldNameToVariableName = (HashMap<String, String>) fieldNameToVariableName.clone();
        }
        copy.areaSelection = areaSelection;
        copy.viewModification = viewModification;
        copy.mapType = mapType;
        copy.areaOption = areaOption;
        copy.pointOption = pointOption;
        if (relevantProxies != null) {
            copy.relevantProxies = (ArrayList<ProxyInt>) relevantProxies.clone();
        }
        return copy;
    }

    public ArrayList<ProxyInt> getRelevantProxies() {
        return relevantProxies;
    }

    public void setRelevantProxies(ArrayList<ProxyInt> relevantProxies) {
        this.relevantProxies = relevantProxies;
    }
}
