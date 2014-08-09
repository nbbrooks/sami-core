package sami.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import sami.mission.MissionPlanSpecification;

/**
 *
 * @author nbb
 */
public class CheckReturn extends InputEvent {

    // List of fields for which a definition should be provided
    public static final ArrayList<String> fieldNames = new ArrayList<String>();
    // Description for each field
    public static final HashMap<String, String> fieldNameToDescription = new HashMap<String, String>();
    // List of fields for which a variable name should be provided
    public static final ArrayList<String> variableNames = new ArrayList<String>();
    // Description for each variable
    public static final HashMap<String, String> variableNameToDescription = new HashMap<String, String>();
    // Fields
    public MissionPlanSpecification subMission;
    public String variableValue;
    // Hidden fields
    String variableName;

    static {
        fieldNames.add("subMission");
        fieldNames.add("variableValue");

        fieldNameToDescription.put("subMission", "Sub-mission to check return value of?");
        fieldNameToDescription.put("variableValue", "Variable value?");
    }

    public CheckReturn() {
        id = UUID.randomUUID();
    }

    public CheckReturn(UUID relevantOutputEventUuid, UUID missionUuid, MissionPlanSpecification subMission, String variableValue) {
        this.relevantOutputEventId = relevantOutputEventUuid;
        this.missionId = missionUuid;
        id = UUID.randomUUID();
        this.subMission = subMission;
        this.variableValue = variableValue;
    }

    public CheckReturn(CheckReturn template, MissionPlanSpecification subMission, String variableName) {
        this.relevantOutputEventId = template.getRelevantOutputEventId();
        this.missionId = template.getMissionId();
        id = UUID.randomUUID();
        this.subMission = subMission;
        this.variableValue = template.variableValue;
        this.variableName = variableName;
        active = true;
    }

    public MissionPlanSpecification getMSpec() {
        return subMission;
    }

    public String getVariableValue() {
        return variableValue;
    }

    public String getVariableName() {
        return variableName;
    }

    public String toString() {
        return "CheckReturn [" + (subMission == null ? subMission : subMission.getName()) + ", " + variableValue + "]";
    }
}
