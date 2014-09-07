package sami.allocation;

import com.perc.mitpas.adi.common.datamodels.AbstractAsset;
import com.perc.mitpas.adi.mission.planning.task.ITask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author nbb
 */
public class ResourceAllocation {

    Map<ITask, AbstractAsset> taskToAsset;
    Map<AbstractAsset, ArrayList<ITask>> assetToTasks;
    ArrayList<ITask> unallocatedTasks;
    Map<ITask, Long> taskToTime;

    private ResourceAllocation() {
    }

    public ResourceAllocation(Map<ITask, AbstractAsset> taskToAsset, final Map<ITask, Long> taskToTime) {
        this.taskToAsset = taskToAsset;
        this.taskToTime = taskToTime;
        // Fill out remaining fields
        assetToTasks = new HashMap<AbstractAsset, ArrayList<ITask>>();
        unallocatedTasks = new ArrayList<ITask>();
        for (ITask task : taskToAsset.keySet()) {
            AbstractAsset asset = taskToAsset.get(task);
            if (asset == null) {
                unallocatedTasks.add(task);
            } else {
                ArrayList<ITask> taskList;
                if (assetToTasks.containsKey(asset)) {
                    taskList = assetToTasks.get(asset);
                } else {
                    taskList = new ArrayList<ITask>();
                    assetToTasks.put(asset, taskList);
                }
                taskList.add(task);
            }
        }
        // Now sort each entry in assetToTasks
        for (AbstractAsset asset : assetToTasks.keySet()) {
            ArrayList<ITask> taskList = assetToTasks.get(asset);
            Collections.sort(taskList, new Comparator<ITask>() {
                @Override
                public int compare(ITask task1, ITask task2) {
                    if (!taskToTime.containsKey(task1) || !taskToTime.containsKey(task2)) {
                        return 0;
                    }
                    return (int) (taskToTime.get(task1) - taskToTime.get(task2));
                }
            });
        }
    }

    public ResourceAllocation(Map<ITask, AbstractAsset> taskToAsset, Map<AbstractAsset, ArrayList<ITask>> assetToTasks, ArrayList<ITask> unallocatedTasks, Map<ITask, Long> taskToTime) {
        this.taskToAsset = taskToAsset;
        this.assetToTasks = assetToTasks;
        this.unallocatedTasks = unallocatedTasks;
        this.taskToTime = taskToTime;
    }

    public Map<ITask, AbstractAsset> getTaskToAsset() {
        return taskToAsset;
    }

    public Map<AbstractAsset, ArrayList<ITask>> getAssetToTasks() {
        return assetToTasks;
    }

    public ArrayList<ITask> getUnallocatedTasks() {
        return unallocatedTasks;
    }

    public Map<ITask, Long> getTaskToTime() {
        return taskToTime;
    }

    @Override
    public ResourceAllocation clone() {
        ResourceAllocation clone = new ResourceAllocation();
        clone.taskToAsset = new HashMap<ITask, AbstractAsset>();
        clone.assetToTasks = new HashMap<AbstractAsset, ArrayList<ITask>>();
        clone.unallocatedTasks = (ArrayList<ITask>) unallocatedTasks.clone();
        for (ITask iTask : taskToAsset.keySet()) {
            clone.taskToAsset.put(iTask, taskToAsset.get(iTask));
        }
        for (AbstractAsset asset : assetToTasks.keySet()) {
            clone.assetToTasks.put(asset, (ArrayList<ITask>) assetToTasks.get(asset).clone());
        }
        if (taskToTime != null) {
            clone.taskToTime = new HashMap<ITask, Long>();
            for (ITask iTask : taskToTime.keySet()) {
                clone.taskToTime.put(iTask, taskToTime.get(iTask));
            }
        }
        return clone;
    }

    public String toString() {
        String ret = "";
        for (AbstractAsset asset : assetToTasks.keySet()) {
            ret += "\n\t" + asset.getName() + " -> " + assetToTasks.get(asset);
        }
        return ret;
    }
}
