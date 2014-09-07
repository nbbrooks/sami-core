package sami.engine;

import com.perc.mitpas.adi.mission.planning.task.ITask;
import sami.allocation.ResourceAllocation;

/**
 *
 * @author pscerri
 */
public interface TaskAllocationListenerInt {

    public void allocationApplied(ResourceAllocation resourceAllocation);

    public void taskCompleted(ITask task);
}
