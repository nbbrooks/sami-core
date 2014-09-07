package sami.event;

import com.perc.mitpas.adi.mission.planning.task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.proxy.ProxyInt;

/**
 * Used to indicate that a proxy is no longer responsible for a task, as it has
 * been reassigned to a different proxy
 *
 * A separate TaskReassigned input event is used to notify the new proxy
 * responsible for the task
 *
 * @author nbb
 */
public class TaskReleased extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Fields
    protected Task task;

    public TaskReleased() {
        id = UUID.randomUUID();
    }

    public TaskReleased(UUID missionUuid, ProxyInt proxy, Task task) {
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        this.task = task;
        relevantProxyList = new ArrayList<ProxyInt>();
        relevantProxyList.add(proxy);
        relevantTaskList = new ArrayList<Task>();
        relevantTaskList.add(task);
    }
    
    public Task getTask() {
        return task;
    }
}
