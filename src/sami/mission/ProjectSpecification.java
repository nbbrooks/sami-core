package sami.mission;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiElementSpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import sami.event.InputEvent;

/**
 *
 * @author pscerri
 */
public class ProjectSpecification implements java.io.Serializable {

    private static final Logger LOGGER = Logger.getLogger(ProjectSpecification.class.getName());
    static final long serialVersionUID = 0L;
    // @todo needsSaving only takes into account added and changed specs, not any details
    private boolean needsSaving = false;
    // Temp
    // @todo Do a proper GUI specification
    private ArrayList<GuiElementSpec> guiElements = null;
    private ArrayList<RequirementSpecification> reqs;
    private DefaultMutableTreeNode missionTree = new DefaultMutableTreeNode("Plays");
    private ArrayList<TestCase> testCases = null;
    private HashMap<String, Object> globalVariables = new HashMap<String, Object>();
    transient private ArrayList<MissionPlanSpecification> allMissionPlans = new ArrayList<MissionPlanSpecification>();
    transient private ArrayList<MissionPlanSpecification> rootMissionPlans = new ArrayList<MissionPlanSpecification>();
    transient private HashMap<MissionPlanSpecification, DefaultMutableTreeNode> mSpecToNode = new HashMap<MissionPlanSpecification, DefaultMutableTreeNode>();
    transient private HashMap<DefaultMutableTreeNode, MissionPlanSpecification> nodeToMSpec = new HashMap<DefaultMutableTreeNode, MissionPlanSpecification>();

    public DefaultMutableTreeNode addRootMissionPlan(MissionPlanSpecification mSpec) {
        // Root mission
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(mSpec);
        mSpecToNode.put(mSpec, node);
        nodeToMSpec.put(node, mSpec);
        missionTree.add(node);
        allMissionPlans.add(mSpec);
        rootMissionPlans.add(mSpec);
        needsSaving = true;
        return node;
    }

    public DefaultMutableTreeNode addSubMissionPlan(MissionPlanSpecification childMSpec, MissionPlanSpecification parentMSpec) {
        DefaultMutableTreeNode parentNode = mSpecToNode.get(parentMSpec);
        return addSubMissionPlan(childMSpec, parentNode);
    }

    public DefaultMutableTreeNode addSubMissionPlan(MissionPlanSpecification childMSpec, DefaultMutableTreeNode parentNode) {
        // Sub-mission
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childMSpec);
        mSpecToNode.put(childMSpec, childNode);
        nodeToMSpec.put(childNode, childMSpec);
        parentNode.add(childNode);
        allMissionPlans.add(childMSpec);
        needsSaving = true;
        return childNode;
    }

    public void removeMissionPlan(MissionPlanSpecification mps) {
        DefaultMutableTreeNode node = mSpecToNode.get(mps);
        if (node != null) {
            removeMissionPlanNode(node);
            needsSaving = true;
        } else {
            LOGGER.warning("Tried to remove mSpec: " + mps + ", but it could not be found");
        }
    }

    public void removeMissionPlanNode(DefaultMutableTreeNode missionNode) {
        // First remove any subplans
        MissionPlanSpecification mSpec = nodeToMSpec.get(missionNode);
        for (Vertex v : mSpec.getGraph().getVertices()) {
            if (v instanceof Place && ((Place) v).getSubMissions() != null) {
                for (MissionPlanSpecification subMSpec : ((Place) v).getSubMissions()) {
                    DefaultMutableTreeNode subMNode = mSpecToNode.get(subMSpec);
                    removeMissionPlanNode(subMNode);
                }
            }
        }
        // Remove plan
        nodeToMSpec.remove(missionNode);
        mSpecToNode.remove(mSpec);
        ((DefaultMutableTreeNode) missionNode.getParent()).remove(missionNode);
        allMissionPlans.remove(mSpec);
        rootMissionPlans.remove(mSpec);
        needsSaving = true;
    }

    public MissionPlanSpecification getNewMissionPlanSpecification(String name) {
        MissionPlanSpecification spec = new MissionPlanSpecification(name);
        return spec;
    }

    public ArrayList<MissionPlanSpecification> getAllMissionPlans() {
        return allMissionPlans;
    }

    public ArrayList<MissionPlanSpecification> getRootMissionPlans() {
        return rootMissionPlans;
    }

    public DefaultMutableTreeNode getMissionTree() {
        return missionTree;
    }

    public DefaultMutableTreeNode getNode(MissionPlanSpecification mSpec) {
        return mSpecToNode.get(mSpec);
    }

    public ArrayList<RequirementSpecification> getReqs() {
        return reqs;
    }

    public void setReqs(ArrayList<RequirementSpecification> reqs) {
        this.reqs = reqs;
    }

    public ArrayList<sami.gui.GuiElementSpec> getGuiElements() {
        return guiElements;
    }

    public void setGuiElements(ArrayList<GuiElementSpec> elements) {
        needsSaving = true;
        guiElements = elements;
    }

    public boolean needsSaving() {
        return needsSaving;
    }

    public void saved() {
        needsSaving = false;
    }

    public ArrayList<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(ArrayList<TestCase> testCases) {
        this.testCases = testCases;
    }

    public ArrayList<String> getVariables(Field targetField) {
        ArrayList<String> varNames = new ArrayList<String>();

        for (MissionPlanSpecification mSpec : allMissionPlans) {
            if (mSpec.getGraph() != null && mSpec.getGraph().getEdges() != null) {
                for (Vertex v : mSpec.getGraph().getVertices()) {
                    if (v instanceof Transition) {
                        if (mSpec.getEventSpecList((Transition) v) != null) {
                            for (ReflectedEventSpecification eventSpec : mSpec.getEventSpecList((Transition) v)) {
                                // This is in place of actually working out whether this is an input or output event
                                if (eventSpec.getWriteVariables() != null) {
                                    Class c;
                                    Hashtable<String, Class> paramToClass;

                                    try {
                                        c = Class.forName(eventSpec.getClassName());
                                        paramToClass = ((InputEvent) c.newInstance()).getInputEventDataTypes(c);
                                        if (paramToClass.size() > 0) {
                                            for (String fieldName : eventSpec.getWriteVariables().keySet()) {
                                                if (targetField.getDeclaringClass().isAssignableFrom(paramToClass.get(fieldName)) || targetField.getType().isAssignableFrom(paramToClass.get(fieldName))) {
                                                    String varName = eventSpec.getWriteVariables().get(fieldName);
                                                    if (!varNames.contains(varName)) {
                                                        varNames.add(eventSpec.getWriteVariables().get(fieldName));
                                                    }
                                                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Adding " + eventSpec.getWriteVariables().get(fieldName) + "(" + fieldName + ") to variable list");
                                                }
                                            }

                                        }
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    } catch (InstantiationException e) {
                                        e.printStackTrace();
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "No events to check for variables.");
                        }
                    }
                }
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "No edges to check for events");
            }
        }
        for (String variable : globalVariables.keySet()) {
            if (targetField.getDeclaringClass().isAssignableFrom(globalVariables.get(variable).getClass()) || targetField.getType().isAssignableFrom(globalVariables.get(variable).getClass())) {
                if (!varNames.contains(variable)) {
                    varNames.add(variable);
                }
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Adding " + variable + " to variable list");
            }
        }
        return varNames;
    }
    
    public boolean isGlobalVariable(String variable) {
        return globalVariables.containsKey(variable);
    }
    
    public void deleteGlobalVariable(String variable) {
        globalVariables.remove(variable);
    }
    
    public Object getGlobalVariableValue(String variable) {
        return globalVariables.get(variable);
    }
    
    public void setGlobalVariableValue(String variable, Object value) {
        globalVariables.put(variable, value);
    }
    
    public HashMap<String, Object> getGlobalVariableToValue() {
        return globalVariables;
    }

    public void printDetails() {
        Enumeration treeEnum = missionTree.breadthFirstEnumeration();
        while (treeEnum.hasMoreElements()) {
            Object node = treeEnum.nextElement();
            if (node instanceof MissionPlanSpecification) {
                MissionPlanSpecification missionSpec = (MissionPlanSpecification) node;
                System.out.println("missionSpec " + missionSpec.getName());
                for (Vertex v : missionSpec.getGraph().getVertices()) {
                    System.out.println("\tvertex " + v.getTag());
                    if (missionSpec.getEventSpecList(v) == null) {
                        System.out.println("\t\tNULL");
                        continue;
                    }
                    for (ReflectedEventSpecification eventSpec : missionSpec.getEventSpecList(v)) {
                        System.out.println("\t\teventSpec " + eventSpec);
                        HashMap<String, Object> fieldValues = eventSpec.getFieldValues();
                        for (String fieldName : fieldValues.keySet()) {
                            System.out.println("\t\t\t<field, value> = " + fieldName + " -> " + fieldValues.get(fieldName));
                        }
                        HashMap<String, String> readVariables = eventSpec.getReadVariables();
                        for (String fieldName : readVariables.keySet()) {
                            System.out.println("\t\t\t<field, read var> = " + fieldName + " -> " + readVariables.get(fieldName));
                        }
                        HashMap<String, String> writeVariables = eventSpec.getWriteVariables();
                        for (String fieldName : writeVariables.keySet()) {
                            System.out.println("\t\t\t<field, write var> = " + fieldName + " -> " + writeVariables.get(fieldName));
                        }
                    }
                }
            }
        }
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            if(globalVariables == null) {
                globalVariables = new HashMap<String, Object>();
            }
            // Populate mSpecToNode, nodeToMSpec, and allMissionPlans
            mSpecToNode = new HashMap<MissionPlanSpecification, DefaultMutableTreeNode>();
            nodeToMSpec = new HashMap<DefaultMutableTreeNode, MissionPlanSpecification>();
            allMissionPlans = new ArrayList<MissionPlanSpecification>();
            rootMissionPlans = new ArrayList<MissionPlanSpecification>();
            Enumeration e = missionTree.breadthFirstEnumeration();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                if (node.getUserObject() instanceof MissionPlanSpecification) {
                    mSpecToNode.put((MissionPlanSpecification) node.getUserObject(), node);
                    nodeToMSpec.put(node, (MissionPlanSpecification) node.getUserObject());
                    allMissionPlans.add((MissionPlanSpecification) node.getUserObject());
                }
            }
            // Populate rootMissionPlans
            e = missionTree.children();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                if (node.getUserObject() instanceof MissionPlanSpecification) {
                    rootMissionPlans.add((MissionPlanSpecification) node.getUserObject());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
