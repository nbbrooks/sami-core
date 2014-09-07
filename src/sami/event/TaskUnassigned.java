
package sami.event;

import com.perc.mitpas.adi.mission.planning.task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Used to indicate that NO proxy is responsible for this task, as a task re-allocation could not find an assignment for it
 * 
 * @author nbb
 */
public class TaskUnassigned extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public TaskUnassigned() {
        id = UUID.randomUUID();
    }

    public TaskUnassigned(UUID missionUuid, Task task) {
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        relevantTaskList = new ArrayList<Task>();
        relevantTaskList.add(task);
    }
}