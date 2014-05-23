package sami.mission;

import sami.event.ReflectedEventSpecification;
import sami.gui.GuiElementSpec;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author pscerri
 */
public class ProjectSpecification implements java.io.Serializable {

    static final long serialVersionUID = 0L;
    // @todo needsSaving only takes into account added and changed specs, not any details
    private boolean needsSaving = false;
    // Temp
    // @todo Do a proper GUI specification
    private ArrayList<GuiElementSpec> guiElements = null;
    private ArrayList<RequirementSpecification> reqs;
    ArrayList<MissionPlanSpecification> missionPlans = new ArrayList<MissionPlanSpecification>();
    private ArrayList<TestCase> testCases = null;

    public ArrayList<RequirementSpecification> getReqs() {
        return reqs;
    }

    public void setReqs(ArrayList<RequirementSpecification> reqs) {
        this.reqs = reqs;
    }

    public void addMissionPlan(MissionPlanSpecification m) {
        if (!missionPlans.contains(m)) {
            missionPlans.add(m);
        }
        needsSaving = true;
    }

    public ArrayList<MissionPlanSpecification> getMissionPlans() {
        return missionPlans;
    }

    public MissionPlanSpecification getNewMissionPlanSpecification(String name) {
        MissionPlanSpecification spec = new MissionPlanSpecification(name);
        missionPlans.add(spec);
        needsSaving = true;
        return spec;
    }

    public void setGuiElements(ArrayList<GuiElementSpec> elements) {
        needsSaving = true;
        guiElements = elements;
    }

    public ArrayList<sami.gui.GuiElementSpec> getGuiElements() {
        return guiElements;
    }

    public boolean needsSaving() {
        return needsSaving;
    }

    public void saved() {
        needsSaving = false;
    }

    public void removeMissionPlan(MissionPlanSpecification mps) {
        missionPlans.remove(mps);
        needsSaving = true;
    }

    public ArrayList<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(ArrayList<TestCase> testCases) {
        this.testCases = testCases;
    }

    public void printDetails() {
        for (MissionPlanSpecification missionSpec : missionPlans) {
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
