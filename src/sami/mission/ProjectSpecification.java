package sami.mission;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiElementSpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import sami.variable.VariableReference;

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
    protected long lastElementId = 0;
    transient private ArrayList<MissionPlanSpecification> allMissionPlans = new ArrayList<MissionPlanSpecification>();
    transient private ArrayList<MissionPlanSpecification> rootMissionPlans = new ArrayList<MissionPlanSpecification>();
    transient private HashMap<MissionPlanSpecification, MissionPlanSpecification> childToParent = new HashMap<MissionPlanSpecification, MissionPlanSpecification>();
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

    public long getLastElementId() {
        return lastElementId;
    }

    public long getAndIncLastElementId() {
        return lastElementId++;
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
        if (parentNode.getUserObject() instanceof MissionPlanSpecification) {
            childToParent.put(childMSpec, (MissionPlanSpecification) parentNode.getUserObject());
        }
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
        for (Vertex v : mSpec.getTransientGraph().getVertices()) {
            if (v instanceof Place && ((Place) v).getSubMissionTemplates() != null) {
                for (MissionPlanSpecification subMSpec : ((Place) v).getSubMissionTemplates()) {
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

    public ArrayList<String> getVariablesInScope(MissionPlanSpecification mSpecScope) {
        ArrayList<String> useableVariableNames = new ArrayList<String>();

        // Add variables in current PM, if one exists
        if (mSpecScope != null) {
            ArrayList<String> mSpecVariableNames = getMissionSpecVariables(mSpecScope);
            for (String variableName : mSpecVariableNames) {
                useableVariableNames.add(variableName);
            }
            // Add variables from parent PMs, if any exist
            MissionPlanSpecification recursiveScope = mSpecScope;
            while (childToParent.containsKey(recursiveScope) && childToParent.get(recursiveScope) != null) {
                recursiveScope = childToParent.get(recursiveScope);
                mSpecVariableNames = getMissionSpecVariables(recursiveScope);
                for (String variableName : mSpecVariableNames) {
                    if (!useableVariableNames.contains(variableName)) {
                        useableVariableNames.add(variableName);
                    }
                }
            }
        }
        // Add all global variables
        for (String variableName : globalVariables.keySet()) {
            if (!useableVariableNames.contains(variableName)) {
                useableVariableNames.add(variableName);
            }
        }

        return useableVariableNames;
    }

    private ArrayList<String> getMissionSpecVariables(MissionPlanSpecification mSpec) {
        ArrayList<String> variableNames = new ArrayList<String>();
        if (mSpec == null) {
            LOGGER.warning("Called getMissionSpecVariables with NULL mSpec");
            return variableNames;
        }

        // Add variables in current PM, if one exists
        if (mSpec != null) {
            if (mSpec.getTransientGraph() != null && mSpec.getTransientGraph().getEdges() != null) {
                for (Vertex v : mSpec.getTransientGraph().getVertices()) {
                    if (mSpec.getEventSpecList(v) != null) {
                        for (ReflectedEventSpecification eventSpec : mSpec.getEventSpecList(v)) {
                            HashMap<String, String> writeVariables = eventSpec.getWriteVariables();
                            for (String writeFieldName : writeVariables.keySet()) {
                                String writeVariable = writeVariables.get(writeFieldName);
                                if (!variableNames.contains(writeVariable)) {
                                    variableNames.add(writeVariable);
                                }
                            }
                        }
                    }
                }
            }
        }

        return variableNames;
    }

    public ArrayList<String> getAllVariables() {
        ArrayList<String> useableVariableNames = new ArrayList<String>();

        // Add all  mission variables
        for (MissionPlanSpecification mSpec : allMissionPlans) {
            if (mSpec.getTransientGraph() != null && mSpec.getTransientGraph().getEdges() != null) {
                for (Vertex v : mSpec.getTransientGraph().getVertices()) {
                    if (mSpec.getEventSpecList(v) != null) {
                        for (ReflectedEventSpecification eventSpec : mSpec.getEventSpecList(v)) {
                            HashMap<String, String> writeVariables = eventSpec.getWriteVariables();
                            for (String writeFieldName : writeVariables.keySet()) {
                                String writeVariable = writeVariables.get(writeFieldName);
                                if (!useableVariableNames.contains(writeVariable)) {
                                    useableVariableNames.add(writeVariable);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add all global variables
        useableVariableNames.addAll(globalVariables.keySet());

        return useableVariableNames;
    }

    public ArrayList<String> getMissionSpecVariables(Field targetField, MissionPlanSpecification mSpec) {
        ArrayList<String> variableNames = new ArrayList<String>();
        if (targetField == null) {
            LOGGER.warning("Called getMissionSpecVariables with NULL targetField");
            return variableNames;
        }
        if (mSpec == null) {
            LOGGER.warning("Called getMissionSpecVariables with NULL mSpec");
            return variableNames;
        }

        if (mSpec.getTransientGraph() != null && mSpec.getTransientGraph().getEdges() != null) {
            for (Vertex v : mSpec.getTransientGraph().getVertices()) {
                if (mSpec.getEventSpecList(v) != null) {
                    for (ReflectedEventSpecification eventSpec : mSpec.getEventSpecList(v)) {
                        try {
                            Class eventClass = Class.forName(eventSpec.getClassName());
                            HashMap<String, String> writeVariables = eventSpec.getWriteVariables();
                            for (String writeFieldName : writeVariables.keySet()) {
                                boolean match = false;

                                // For each field with a write variable assigned to it
                                Field writeField = eventClass.getField(writeFieldName);
                                if (java.util.Hashtable.class.isAssignableFrom(targetField.getType()) && java.util.Hashtable.class.isAssignableFrom(writeField.getType())) {
                                    // Hashtable check
                                    if (parameterizedTypeMatch((ParameterizedType) targetField.getGenericType(), (ParameterizedType) writeField.getGenericType())) {
                                        match = true;
                                    }
                                } else if (java.util.List.class.isAssignableFrom(targetField.getType()) && java.util.List.class.isAssignableFrom(writeField.getType())) {
                                    if (parameterizedTypeMatch((ParameterizedType) targetField.getGenericType(), (ParameterizedType) writeField.getGenericType())) {
                                        match = true;
                                    }
                                } else if (targetField.getType() instanceof Class && writeField.getType() instanceof Class) {
                                    if (classTypeMatch((Class) targetField.getType(), (Class) writeField.getType())) {
                                        match = true;
                                    }
                                }
                                if (match) {
                                    String writeVariable = writeVariables.get(writeFieldName);
                                    if (!variableNames.contains(writeVariable)) {
                                        variableNames.add(writeVariable);
                                    }
                                    LOGGER.fine("Adding " + writeVariable + " to variable list");
                                }
                            }
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        } catch (NoSuchFieldException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
        return variableNames;
    }
    
    public ArrayList<String> getMissionSpecVariables(Class targetClass, MissionPlanSpecification mSpec) {
        ArrayList<String> variableNames = new ArrayList<String>();
        if (targetClass == null) {
            LOGGER.warning("Called getMissionSpecVariables with NULL targetClass");
            return variableNames;
        }
        if (mSpec == null) {
            LOGGER.warning("Called getMissionSpecVariables with NULL mSpec");
            return variableNames;
        }

        if (mSpec.getTransientGraph() != null && mSpec.getTransientGraph().getEdges() != null) {
            for (Vertex v : mSpec.getTransientGraph().getVertices()) {
                if (mSpec.getEventSpecList(v) != null) {
                    for (ReflectedEventSpecification eventSpec : mSpec.getEventSpecList(v)) {
                        try {
                            Class eventClass = Class.forName(eventSpec.getClassName());
                            HashMap<String, String> writeVariables = eventSpec.getWriteVariables();
                            for (String writeFieldName : writeVariables.keySet()) {
                                boolean match = false;
                                
                                // For each field with a write variable assigned to it
                                Field writeField = eventClass.getField(writeFieldName);
                                if (java.util.Hashtable.class.isAssignableFrom(targetClass) && java.util.Hashtable.class.isAssignableFrom(writeField.getType())) {
                                    // Hashtable check
                                    LOGGER.warning("Cannot check Hashtable key and value classes from Class, assuming a match");
                                    match = true;
                                } else if (java.util.List.class.isAssignableFrom(targetClass) && java.util.List.class.isAssignableFrom(writeField.getType())) {
                                    // List check
                                    LOGGER.warning("Cannot check List item class from Class, assuming a match");
                                    match = true;
                                } else if (writeField.getType() instanceof Class && (Class) writeField.getType() == VariableReference.class) {
                                    if (classTypeMatch(targetClass, (Class) writeField.getType())) {
                                        match = true;
                                    }
                                } else if (writeField.getType() instanceof Class) {
                                    if (classTypeMatch(targetClass, (Class) writeField.getType())) {
                                        match = true;
                                    }
                                }
                                if (match) {
                                    String writeVariable = writeVariables.get(writeFieldName);
                                    if (!variableNames.contains(writeVariable)) {
                                        variableNames.add(writeVariable);
                                    }
                                    LOGGER.fine("Adding " + writeVariable + " to variable list");
                                }
                            }
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        } catch (NoSuchFieldException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
        return variableNames;
    }

    public ArrayList<String> getVariablesInScope(Field targetField, MissionPlanSpecification mSpecScope) {
        ArrayList<String> useableVariableNames = new ArrayList<String>();

        // Add compatible variables in current PM, if one exists
        if (mSpecScope != null) {
            ArrayList<String> mSpecVariableNames = getMissionSpecVariables(targetField, mSpecScope);
            for (String variableName : mSpecVariableNames) {
                useableVariableNames.add(variableName);
            }
            // Add compatible variables from parent PMs, if any exist
            MissionPlanSpecification recursiveScope = mSpecScope;
            while (childToParent.containsKey(recursiveScope) && childToParent.get(recursiveScope) != null) {
                recursiveScope = childToParent.get(recursiveScope);
                mSpecVariableNames = getMissionSpecVariables(targetField, recursiveScope);
                for (String variableName : mSpecVariableNames) {
                    if (!useableVariableNames.contains(variableName)) {
                        useableVariableNames.add(variableName);
                    }
                }
            }
        }
        // Add compatible global variables
        for (String variable : globalVariables.keySet()) {
            Object globalValue = globalVariables.get(variable);
            if(globalValue == null) { 
                LOGGER.severe("Global variable \"" + variable + "\" has NULL definition, skipping");
                continue;
            }
            boolean match = false;

            if (java.util.Hashtable.class.isAssignableFrom(targetField.getType()) && java.util.Hashtable.class.isAssignableFrom(globalValue.getClass())) {
                // Hashtable check
                Class targetKeyClass = null, targetValueClass = null, writeKeyClass = null, writeValueClass = null;
                Type targetGenericType = targetField.getGenericType();
                if (targetGenericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) targetGenericType;
                    Type targetKeyType = parameterizedType.getActualTypeArguments()[0];
                    Type targetValueType = parameterizedType.getActualTypeArguments()[1];

                    if (targetKeyType instanceof Class && targetValueType instanceof Class) {
                        targetKeyClass = (Class) targetKeyType;
                        targetValueClass = (Class) targetValueType;
                    }
                }
                Hashtable hashtable = (Hashtable) globalValue;
                if (!hashtable.isEmpty()) {
                    for (Object key : hashtable.keySet()) {
                        writeKeyClass = key.getClass();
                        writeValueClass = hashtable.get(key).getClass();
                        break;
                    }
                }
                if (targetKeyClass != null && targetValueClass != null && writeKeyClass != null && writeValueClass != null
                        && targetKeyClass.isAssignableFrom(writeKeyClass) && targetValueClass.isAssignableFrom(writeValueClass)) {
                    match = true;
                } else if (java.util.Hashtable.class.isAssignableFrom(targetValueClass)) {
                    LOGGER.warning("Nested Hashtable in global variable value not yet supported");
                } else if (java.util.List.class.isAssignableFrom(targetValueClass)) {
                    LOGGER.warning("Nested List in global variable value not yet supported");
                }

            } else if (java.util.List.class.isAssignableFrom(targetField.getType()) && java.util.List.class.isAssignableFrom(globalValue.getClass())) {
                Class targetClass = null, writeClass = null;
                Type targetGenericType = targetField.getGenericType();
                if (targetGenericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) targetGenericType;
                    Type targetType = parameterizedType.getActualTypeArguments()[0];
                    if (targetType instanceof Class) {
                        targetClass = (Class) targetType;
                    }
                }
                ArrayList arrayList = (ArrayList) globalValue;
                if (!arrayList.isEmpty()) {
                    writeClass = arrayList.get(0).getClass();
                }
                if (targetClass != null && writeClass != null
                        && targetClass.isAssignableFrom(writeClass)) {
                    match = true;
                } else if (java.util.Hashtable.class.isAssignableFrom(targetClass)) {
                    LOGGER.warning("Nested Hashtable in global variable value not yet supported");
                } else if (java.util.List.class.isAssignableFrom(targetClass)) {
                    LOGGER.warning("Nested List in global variable value not yet supported");
                }
            } else if (primitiveTypeMatch(targetField.getType(), globalVariables.get(variable).getClass())) {
                match = true;
            } else if (targetField.getType().isAssignableFrom(globalVariables.get(variable).getClass())) {
                match = true;
            }

            if (match && !useableVariableNames.contains(variable)) {
                useableVariableNames.add(variable);
            }
            LOGGER.fine("Adding " + variable + " to variable list");
        }
        return useableVariableNames;
    }
    
    public ArrayList<String> getVariablesInScope(Class targetClass, MissionPlanSpecification mSpecScope) {
        ArrayList<String> useableVariableNames = new ArrayList<String>();

        // Add compatible variables in current PM, if one exists
        if (mSpecScope != null) {
            ArrayList<String> mSpecVariableNames = getMissionSpecVariables(targetClass, mSpecScope);
            for (String variableName : mSpecVariableNames) {
                useableVariableNames.add(variableName);
            }
            // Add compatible variables from parent PMs, if any exist
            MissionPlanSpecification recursiveScope = mSpecScope;
            while (childToParent.containsKey(recursiveScope) && childToParent.get(recursiveScope) != null) {
                recursiveScope = childToParent.get(recursiveScope);
                mSpecVariableNames = getMissionSpecVariables(targetClass, recursiveScope);
                for (String variableName : mSpecVariableNames) {
                    if (!useableVariableNames.contains(variableName)) {
                        useableVariableNames.add(variableName);
                    }
                }
            }
        }
        // Add compatible global variables
        for (String variable : globalVariables.keySet()) {
            Object globalValue = globalVariables.get(variable);
            if(globalValue == null) { 
                LOGGER.severe("Global variable \"" + variable + "\" has NULL definition, skipping");
                continue;
            }
            boolean match = false;

            if (java.util.Hashtable.class.isAssignableFrom(targetClass) && java.util.Hashtable.class.isAssignableFrom(globalValue.getClass())) {
                // Hashtable check
                LOGGER.warning("Cannot check Hashtable key and value classes from Class, assuming a match");
                match = true;
            } else if (java.util.List.class.isAssignableFrom(targetClass) && java.util.List.class.isAssignableFrom(globalValue.getClass())) {
                // List check
                LOGGER.warning("Cannot check List item class from Class, assuming a match");
                match = true;
            } else if (primitiveTypeMatch(targetClass, globalVariables.get(variable).getClass())) {
                match = true;
            } else if (targetClass.isAssignableFrom(globalVariables.get(variable).getClass())) {
                match = true;
            }

            if (match && !useableVariableNames.contains(variable)) {
                useableVariableNames.add(variable);
            }
            LOGGER.fine("Adding " + variable + " to variable list");
        }
        return useableVariableNames;
    }

    public boolean primitiveTypeMatch(Class classA, Class classB) {
        if ((classA.equals(Byte.class) && classB.equals(byte.class))
                || (classA.equals(byte.class) && classB.equals(Byte.class))
                || (classA.equals(Character.class) && classB.equals(char.class))
                || (classA.equals(char.class) && classB.equals(Character.class))
                || (classA.equals(Double.class) && classB.equals(double.class))
                || (classA.equals(double.class) && classB.equals(Double.class))
                || (classA.equals(Float.class) && classB.equals(float.class))
                || (classA.equals(float.class) && classB.equals(Float.class))
                || (classA.equals(Integer.class) && classB.equals(int.class))
                || (classA.equals(int.class) && classB.equals(Integer.class))
                || (classA.equals(Long.class) && classB.equals(long.class))
                || (classA.equals(long.class) && classB.equals(Long.class))
                || (classA.equals(Short.class) && classB.equals(short.class))
                || (classA.equals(short.class) && classB.equals(Short.class))
                || (classA.equals(Void.class) && classB.equals(void.class))
                || (classA.equals(void.class) && classB.equals(Void.class))) {
            return true;
        }
        return false;
    }

    public boolean parameterizedTypeMatch(ParameterizedType typeA, ParameterizedType typeB) {
        if (java.util.Hashtable.class.isAssignableFrom((Class) typeA.getRawType()) && java.util.Hashtable.class.isAssignableFrom((Class) typeB.getRawType())) {
            // Hashtable check
            boolean matchA = false, matchB = false;
            Type targetKeyType = typeA.getActualTypeArguments()[0];
            Type targetValueType = typeA.getActualTypeArguments()[1];
            Type writeKeyType = typeB.getActualTypeArguments()[0];
            Type writeValueType = typeB.getActualTypeArguments()[1];
            // Key
            if (targetKeyType instanceof Class && writeKeyType instanceof Class) {
                matchA = classTypeMatch((Class) targetKeyType, (Class) writeKeyType);
            } else if (targetKeyType instanceof ParameterizedType && writeKeyType instanceof ParameterizedType) {
                matchA = parameterizedTypeMatch((ParameterizedType) targetKeyType, (ParameterizedType) writeKeyType);
            }
            // Value
            if (targetValueType instanceof Class && writeValueType instanceof Class) {
                matchB = classTypeMatch((Class) targetValueType, (Class) writeValueType);
            } else if (targetValueType instanceof ParameterizedType && writeValueType instanceof ParameterizedType) {
                matchB = parameterizedTypeMatch((ParameterizedType) targetValueType, (ParameterizedType) writeValueType);
            }
            return matchA && matchB;
        } else if (java.util.List.class.isAssignableFrom((Class) typeA.getRawType()) && java.util.List.class.isAssignableFrom((Class) typeB.getRawType())) {
            // List check
            boolean match = false;
            Type targetType = typeA.getActualTypeArguments()[0];
            Type writeType = typeB.getActualTypeArguments()[0];
            if (targetType instanceof Class && writeType instanceof Class) {
                match = classTypeMatch((Class) targetType, (Class) writeType);
            } else if (targetType instanceof ParameterizedType && writeType instanceof ParameterizedType) {
                match = parameterizedTypeMatch((ParameterizedType) targetType, (ParameterizedType) writeType);
            }
            return match;
        }
        return false;
    }

    public boolean classTypeMatch(Class classA, Class classB) {
        return classA.isAssignableFrom(classB);
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
                for (Vertex v : missionSpec.getTransientGraph().getVertices()) {
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

    public void updateMissionTags() {
        for (MissionPlanSpecification mSpec : allMissionPlans) {
            mSpec.updateTags();
        }
        needsSaving = true;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        try {
            ois.defaultReadObject();
            if (globalVariables == null) {
                globalVariables = new HashMap<String, Object>();
            }
            // Populate mSpecToNode, nodeToMSpec, allMissionPlans, rootMissionPlans, and childToParent
            mSpecToNode = new HashMap<MissionPlanSpecification, DefaultMutableTreeNode>();
            nodeToMSpec = new HashMap<DefaultMutableTreeNode, MissionPlanSpecification>();
            allMissionPlans = new ArrayList<MissionPlanSpecification>();
            rootMissionPlans = new ArrayList<MissionPlanSpecification>();
            childToParent = new HashMap<MissionPlanSpecification, MissionPlanSpecification>();
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
                    // Populate childToParent
                    for (int i = 0; i < node.getChildCount(); i++) {
                        if (node.getChildAt(i) instanceof DefaultMutableTreeNode) {
                            addParentLookup(node, (DefaultMutableTreeNode) node.getChildAt(i));
                        }
                    }
                }
            }
            // Check if vertex IDs have not been assigned (old DRM version)
            if (lastElementId == 0) {
                lastElementId = 1;
                for (MissionPlanSpecification mSpec : allMissionPlans) {
                    for (Vertex vertex : mSpec.getTransientGraph().getVertices()) {
                        vertex.vertexId = lastElementId;
                        lastElementId++;
                    }
                    for (Edge edge : mSpec.getTransientGraph().getEdges()) {
                        edge.edgeId = lastElementId;
                        lastElementId++;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.severe("IO Exception in ProjectSpecification readObject");
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Class Not Found Exception in ProjectSpecification readObject");
            throw e;
        }
    }

    private void addParentLookup(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode childNode) {
        if (parentNode.getUserObject() instanceof MissionPlanSpecification && childNode.getUserObject() instanceof MissionPlanSpecification) {
            childToParent.put((MissionPlanSpecification) childNode.getUserObject(), (MissionPlanSpecification) parentNode.getUserObject());
            for (int i = 0; i < childNode.getChildCount(); i++) {
                if (childNode.getChildAt(i) instanceof DefaultMutableTreeNode) {
                    addParentLookup(childNode, (DefaultMutableTreeNode) childNode.getChildAt(i));
                }
            }
        }
    }

    public static void main(String[] args) {
        Hashtable h = new Hashtable<ArrayList<Double>, Hashtable<Double, Double>>();
        ArrayList<Double> a = new ArrayList<Double>();
        a.add(new Double(0));
        Hashtable h2 = new Hashtable<Double, Double>();
        h2.put(2, 2);
        h.put(a, h2);
    }
}
