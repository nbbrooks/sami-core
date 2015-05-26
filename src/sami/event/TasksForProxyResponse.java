
package sami.event;

import com.perc.mitpas.adi.mission.planning.task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Contains task tokens present in the indicated mission which the proxy is responsible
 * 
 * @author nbb
 */
public class TasksForProxyResponse extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    

    public TasksForProxyResponse() {
    }

    public TasksForProxyResponse(UUID relevantOutputEventUuid, UUID missionUuid, ArrayList<Task> relevantTaskList) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        this.relevantTaskList = relevantTaskList;
        id = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return "TasksForProxyResponse [ " + relevantTaskList + "]";
    }
}
