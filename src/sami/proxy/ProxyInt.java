package sami.proxy;

import com.perc.mitpas.adi.mission.planning.task.Task;
import sami.event.OutputEvent;
import java.util.ArrayList;
import java.util.UUID;
import sami.event.InputEvent;

/**
 *
 * @author pscerri
 */
public interface ProxyInt {

    public int getProxyId();

    public String getProxyName();

    public void addListener(ProxyListenerInt l);

    public void removeListener(ProxyListenerInt l);

    public void start();

    public void handleEvent(OutputEvent oe, Task task);

    public OutputEvent getCurrentEvent();

    public ArrayList<OutputEvent> getEvents();

    public void abortEvent(UUID eventId);

    public void addChildTask(Task parentTask, Task childTask);

    public Task getCurrentTask();

    public ArrayList<Task> getTasks();

    public ArrayList<InputEvent> setTasks(ArrayList<Task> tasks);

    public void taskCompleted(Task task);

    public void abortMission(UUID missionId);

    public void completeMission(UUID missionId);
}
