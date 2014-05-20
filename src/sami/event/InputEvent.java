package sami.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
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
    HashMap<String, String> fieldNameToVariableName = null;
    protected ArrayList<ProxyInt> relevantProxyList = null;
    protected ResourceAllocation allocation = null;
    protected UUID relevantOutputEventId;
    protected boolean blocking = false;

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

    public static Hashtable<String, Class> getInputEventDataTypes(Class actualClass) {

        Hashtable<String, Class> paramToClass = new Hashtable<String, Class>();

        // "Clever" hack from StackOverflow to get the class from this static method
        // Class thisClass = new Object() {}.getClass().getEnclosingClass();
        Field[] fs = actualClass.getFields();

        // System.out.println("Fields size : " + fs.length);
        for (Field field : fs) {
            paramToClass.put(field.getName(), field.getType());
        }

        return paramToClass;
    }

    public HashMap<String, String> getVariables() {
        return fieldNameToVariableName;
    }

    public void setVariables(HashMap<String, String> variables) {
        this.fieldNameToVariableName = variables;
    }

    public void addVariable(String fieldName, String variableName) {
        if (fieldNameToVariableName == null) {
            fieldNameToVariableName = new HashMap<String, String>();
        }
        fieldNameToVariableName.put(fieldName, variableName);
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

    @Override
    public Object deepCopy() {
        InputEvent copy = (InputEvent) super.deepCopy();
        copy.generatorEvent = generatorEvent;
        if (fieldNameToVariableName != null) {
            for (String key : fieldNameToVariableName.keySet()) {
                copy.fieldNameToVariableName.put(key, fieldNameToVariableName.get(key));
            }
        }
        if (relevantProxyList != null) {
            // We need consistent tokens between mission specifications to be able to match tokens to edge requirements
            copy.relevantProxyList = new ArrayList<ProxyInt>();
            for (ProxyInt proxy : relevantProxyList) {
                copy.relevantProxyList.add(proxy);
            }
        }
        if (allocation != null) {
            copy.allocation = allocation.clone();
        }
        return copy;
    }
}
