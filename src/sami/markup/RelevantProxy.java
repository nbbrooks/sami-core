package sami.markup;

import java.util.ArrayList;
import java.util.HashMap;
import sami.proxy.ProxyInt;

/**
 *
 * @author nbb
 */
public class RelevantProxy extends Markup {

    // List of enum fields for which an enum option should be selected
    public static final ArrayList<String> enumFieldNames = new ArrayList<String>();
    // Description for each enum field
    public static final HashMap<String, String> enumNameToDescription = new HashMap<String, String>();
    // Mapping from enum value to the MarkupOption field it requires
    public static final HashMap<Enum, String> enumValueToFieldName = new HashMap<Enum, String>();
    // Fields
    public Proxies proxies;
    public ShowPaths showPaths;

    ArrayList<ProxyInt> relevantProxies;

    public enum Proxies {

        ALL_PROXIES, RELEVANT_PROXIES
    };

    public enum ShowPaths {

        YES, NO
    };

    static {
        enumFieldNames.add("proxies");
        enumFieldNames.add("showPaths");

        enumNameToDescription.put("proxies", "Which proxies are relevant?");
        enumNameToDescription.put("showPaths", "Show proxies' paths?");

        enumValueToFieldName.put(Proxies.ALL_PROXIES, null);
        enumValueToFieldName.put(Proxies.RELEVANT_PROXIES, null);
        enumValueToFieldName.put(ShowPaths.NO, null);
        enumValueToFieldName.put(ShowPaths.YES, null);
    }

    public RelevantProxy() {
    }

    @Override
    public RelevantProxy copy() {
        RelevantProxy copy = new RelevantProxy();
        if (fieldNameToVariableName != null) {
            copy.fieldNameToVariableName = (HashMap<String, String>) fieldNameToVariableName.clone();
        }

        copy.proxies = proxies;
        copy.showPaths = showPaths;
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
