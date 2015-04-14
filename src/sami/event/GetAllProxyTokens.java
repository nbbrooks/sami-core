package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author nbb
 */
public class GetAllProxyTokens extends OutputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // Fields

    public GetAllProxyTokens() {
        id = UUID.randomUUID();
    }

    public String toString() {
        return "GetAllProxyTokens";
    }
}
