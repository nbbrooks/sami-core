package sami.event;

import com.perc.mitpas.adi.mission.planning.task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.allocation.ResourceAllocation;
import sami.proxy.ProxyInt;

/**
 *
 * @author pscerri
 */
public class InputEvent extends Event {

    /**
     * This is an ugly and hopefully temporary hack VBSEventMapperImplementation
     * wants to return the original event to make working out which transition
     * triggered easy but that means any parameters set on the InputEvent don't
     * get to the PlanManager by having this here, PlanManager can get to them.
     */
    private InputEvent generatorEvent = null;
    // Class field name to user defined variable name
    HashMap<String, String> fieldNameToWriteVariable = null;
    protected ArrayList<ProxyInt> relevantProxyList = null;
    protected ArrayList<Task> relevantTaskList = null;
    protected ResourceAllocation allocation = null;
    protected UUID relevantOutputEventId;
    public boolean blocking = false;
    // For run-time visualization: whether the event is considered when matching generated events
    protected boolean active = false;
    // For run-time visualization: whether the event has been completed
    protected boolean status = false;

    public InputEvent() {
    }

    public InputEvent getGeneratorEvent() {
        return generatorEvent;
    }

    /**
     * This allows access to the parameters that caused the InputEvent to be
     * created
     *
     * @param generatorEvent
     */
    public void setGeneratorEvent(InputEvent generatorEvent) {
        this.generatorEvent = generatorEvent;
    }

    public HashMap<String, String> getWriteVariables() {
        return fieldNameToWriteVariable;
    }

    public void setWriteVariables(HashMap<String, String> variables) {
        this.fieldNameToWriteVariable = variables;
    }

    public void addWriteVariable(String fieldName, String variableName) {
        if (fieldNameToWriteVariable == null) {
            fieldNameToWriteVariable = new HashMap<String, String>();
        }
        fieldNameToWriteVariable.put(fieldName, variableName);
    }

    public ResourceAllocation getAllocation() {
        return allocation;
    }

    public void setAllocation(ResourceAllocation allocation) {
        this.allocation = allocation;
    }

    public UUID getRelevantOutputEventId() {
        return relevantOutputEventId;
    }

    public void setRelevantOutputEventId(UUID relevantOutputEventUuid) {
        this.relevantOutputEventId = relevantOutputEventUuid;
    }

    public ArrayList<ProxyInt> getRelevantProxyList() {
        return relevantProxyList;
    }

    public void setRelevantProxyList(ArrayList<ProxyInt> relevantProxyList) {
        this.relevantProxyList = relevantProxyList;
    }

    public ArrayList<Task> getRelevantTaskList() {
        return relevantTaskList;
    }

    public void setRelevantTaskList(ArrayList<Task> relevantTaskList) {
        this.relevantTaskList = relevantTaskList;
    }

    public InputEvent copyForProxyTrigger() {
        InputEvent copy = (InputEvent) deepCopy();
        // This will be used as a separate event, so it needs a unique UUID
        copy.id = UUID.randomUUID();
        return copy;
    }

    public boolean getBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public Object deepCopy() {
        InputEvent copy = (InputEvent) super.deepCopy();
        copy.generatorEvent = generatorEvent;
        if (fieldNameToWriteVariable != null) {
            for (String key : fieldNameToWriteVariable.keySet()) {
                copy.fieldNameToWriteVariable.put(key, fieldNameToWriteVariable.get(key));
            }
        }
        if (relevantProxyList != null) {
            // We need consistent tokens between mission specifications to be able to match tokens to edge requirements
            copy.relevantProxyList = new ArrayList<ProxyInt>();
            for (ProxyInt proxy : relevantProxyList) {
                copy.relevantProxyList.add(proxy);
            }
        }
        if (relevantTaskList != null) {
            copy.relevantTaskList = new ArrayList<Task>();
            for (Task task : relevantTaskList) {
                copy.relevantTaskList.add(task);
            }
        }
        if (allocation != null) {
            copy.allocation = allocation.clone();
        }
        copy.active = active;
        copy.status = status;
        return copy;
    }
}
