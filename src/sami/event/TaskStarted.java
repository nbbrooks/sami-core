package sami.event;

import com.perc.mitpas.adi.mission.planning.task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Used to indicate a proxy has started executing events for a new task
 * 
 * @author nbb
 */
public class TaskStarted extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();

    public TaskStarted() {
        id = UUID.randomUUID();
    }

    public TaskStarted(UUID missionUuid, Task task) {
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        relevantTaskList = new ArrayList<Task>();
        relevantTaskList.add(task);
    }
}