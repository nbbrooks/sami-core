package sami.mission;

import sami.event.OutputEvent;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import sami.CoreHelper;
import sami.markup.ReflectedMarkupSpecification;

/**
 *
 * @author pscerri
 */
public class Place extends Vertex {

    private static final Logger LOGGER = Logger.getLogger(Place.class.getName());
    static final long serialVersionUID = 7L;
    // For shared sub-missions:
    //  isEnd: pass tokens up to parent PM, do not terminate SM
    //  isSharedSmEnd: terminate SM
    protected boolean isStart, isEnd, isSharedSmEnd;
    // In and out are switched here as the class naming convention is with respect to Transitions, not Places
    protected ArrayList<OutEdge> inEdges = new ArrayList<OutEdge>();
    protected ArrayList<InEdge> outEdges = new ArrayList<InEdge>();
    protected ArrayList<Transition> inTransitions = new ArrayList<Transition>();
    protected ArrayList<Transition> outTransitions = new ArrayList<Transition>();
    protected ArrayList<MissionPlanSpecification> subMissions = new ArrayList<MissionPlanSpecification>();
    protected HashMap<MissionPlanSpecification, Boolean> subMissionToSharedInstance = new HashMap<MissionPlanSpecification, Boolean>();
    protected HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> subMissionToTaskMap = new HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>>();
    transient protected boolean isActive = false;   // Whether this place has tokens and its output transition's input events are registered
    transient protected ArrayList<OutputEvent> outputEvents = new ArrayList<OutputEvent>();
    transient protected ArrayList<Token> tokens = new ArrayList<Token>();
    transient protected HashMap<MissionPlanSpecification, Boolean> subMissionStatus = new HashMap<MissionPlanSpecification, Boolean>();

    public Place(String name, FunctionMode functionMode, long vertexId) {
        super(name, functionMode, vertexId);
    }

    public void addInEdge(OutEdge edge) {
        inEdges.add(edge);
    }

    public boolean removeInEdge(OutEdge edge) {
        return inEdges.remove(edge);
    }

    public ArrayList<OutEdge> getInEdges() {
        return inEdges;
    }

    public void addOutEdge(InEdge edge) {
        outEdges.add(edge);
    }

    public boolean removeOutEdge(InEdge edge) {
        return outEdges.remove(edge);
    }

    public ArrayList<InEdge> getOutEdges() {
        return outEdges;
    }

    public void addInTransition(Transition t) {
        if (inTransitions.contains(t)) {
            LOGGER.warning("Tried to add pre-existing inTransition: " + t);
            return;
        }
        inTransitions.add(t);
    }

    public void removeInTransition(Transition t) {
        if (!inTransitions.contains(t)) {
            LOGGER.warning("Tried to remove non-existing inTransition: " + t);
            return;
        }
        inTransitions.remove(t);
    }

    public ArrayList<Transition> getInTransitions() {
        return inTransitions;
    }

    public void setInTransitions(ArrayList<Transition> inTransitions) {
        this.inTransitions = inTransitions;
    }

    public void addOutTransition(Transition t) {
        if (outTransitions.contains(t)) {
            LOGGER.warning("Tried to add pre-existing outTransition: " + t);
            return;
        }
        outTransitions.add(t);
    }

    public void removeOutTransition(Transition t) {
        if (!outTransitions.contains(t)) {
            LOGGER.warning("Tried to remove non-existing outTransition: " + t);
            return;
        }
        outTransitions.remove(t);
    }

    public ArrayList<Transition> getOutTransitions() {
        return outTransitions;
    }

    public void setOutTransitions(ArrayList<Transition> outTransitions) {
        this.outTransitions = outTransitions;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setIsEnd(boolean isEnd) {
        this.isEnd = isEnd;
    }

    /**
     *
     * @return If the shared SM should terminate when this place is entered
     */
    public boolean isSharedSmEnd() {
        return isSharedSmEnd;
    }

    /**
     *
     * @param isSharedSmEnd If the shared SM should terminate when this place is
     * entered
     */
    public void setIsSharedSmEnd(boolean isSharedSmEnd) {
        this.isSharedSmEnd = isSharedSmEnd;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setIsStart(boolean isStart) {
        this.isStart = isStart;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        updateTag();
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Called when reading in a spec to run a mission, not when creating the
     * mission in the GUI
     *
     * @param e
     */
    public void addOutputEvent(OutputEvent e) {
        outputEvents.add(e);
    }

    public ArrayList<OutputEvent> getOutputEvents() {
        return outputEvents;
    }

    public ArrayList<MissionPlanSpecification> getSubMissionTemplates() {
        return subMissions;
    }

    public void setSubMissionTemplates(ArrayList<MissionPlanSpecification> subMissions) {
        this.subMissions = subMissions;
        updateTag();
    }

    public HashMap<MissionPlanSpecification, Boolean> getSubMissionToIsSharedInstance() {
        return subMissionToSharedInstance;
    }

    public void setSubMissionToIsSharedInstance(HashMap<MissionPlanSpecification, Boolean> subMissionToSharedInstance) {
        this.subMissionToSharedInstance = subMissionToSharedInstance;
        updateTag();
    }

    public HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> getSubMissionToTaskMap() {
        return subMissionToTaskMap;
    }

    public void setSubMissionToTaskMap(HashMap<MissionPlanSpecification, HashMap<TaskSpecification, TaskSpecification>> subMissionToTaskMap) {
        this.subMissionToTaskMap = subMissionToTaskMap;
        updateTag();
    }

    public void addToken(Token token) {
        tokens.add(token);
        updateTag();
    }

    public boolean removeToken(Token token) {
        boolean success = tokens.remove(token);
        updateTag();
        return success;
    }

    public int removeAllTokens() {
        int numberRemoved = tokens.size();
        tokens.clear();
        updateTag();
        return numberRemoved;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public Shape getShape() {
        if (subMissions == null || subMissions.isEmpty()) {
            return new Ellipse2D.Double(-10, -10, 20, 20);
        } else {
            return new Ellipse2D.Double(-15, -15, 30, 30);
        }
    }

    @Override
    public void updateTag() {
        tag = "<html>";
        shortTag = "<html>";
        if (GuiConfig.DRAW_LABELS && name != null && !name.equals("")) {
            tag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + name + "</font><br>";
            shortTag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + CoreHelper.shorten(name, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
        }
        if (GuiConfig.DRAW_EVENTS) {
            for (ReflectedEventSpecification eventSpec : eventSpecs) {
                try {
                    Class eventClass = Class.forName(eventSpec.getClassName());
                    String simpleName = eventClass.getSimpleName();
                    if (OutputEvent.class.isAssignableFrom(eventClass)) {
                        tag += "<font color=" + GuiConfig.OUTPUT_EVENT_TEXT_COLOR + ">" + simpleName + "</font><br>";
                        shortTag += "<font color=" + GuiConfig.OUTPUT_EVENT_TEXT_COLOR + ">" + CoreHelper.shorten(simpleName, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                    } else {
                        continue;
                    }
                    if (GuiConfig.DRAW_MARKUPS) {
                        for (ReflectedMarkupSpecification markupSpec : eventSpec.getMarkupSpecs()) {
                            Class markupClass = Class.forName(markupSpec.getClassName());
                            tag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\t" + markupClass.getSimpleName() + "</font><br>";
                            shortTag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\t" + CoreHelper.shorten(markupClass.getSimpleName(), GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                        }
                    }
                } catch (ClassNotFoundException cnfe) {
                    cnfe.printStackTrace();
                }
            }
        }
        if (GuiConfig.DRAW_SUB_MISSIONS && subMissions != null && !subMissions.isEmpty()) {
            if (subMissionStatus != null && !subMissionStatus.isEmpty()) {
                // Run-time execution (mSpec template and spawned instances)
                // Add template mission spec names
                tag += "<font color=" + GuiConfig.SUB_MISSION_TEXT_COLOR_TEMPLATE + ">";
                shortTag += "<font color=" + GuiConfig.SUB_MISSION_TEXT_COLOR_TEMPLATE + ">";
                for (MissionPlanSpecification subMission : subMissions) {
                    tag += subMission.getName() + "<br>";
                    shortTag += CoreHelper.shorten(subMission.getName(), GuiConfig.MAX_STRING_LENGTH) + "<br>";
                }
                tag += "</font>";
                shortTag += "</font>";
                // Add spawned instance mission spec names
                for (MissionPlanSpecification subMission : subMissionStatus.keySet()) {
                    String color;
                    if (subMissionStatus.get(subMission)) {
                        color = GuiConfig.SUB_MISSION_TEXT_COLOR_COMPLETE;
                    } else {
                        color = GuiConfig.SUB_MISSION_TEXT_COLOR_INCOMPLETE;
                    }
                    tag += "<font color=" + color + ">";
                    shortTag += "<font color=" + color + ">";
                    tag += subMission.getName() + "<br>";
                    shortTag += CoreHelper.shorten(subMission.getName(), GuiConfig.MAX_STRING_LENGTH) + "<br>";
                    tag += "</font>";
                    shortTag += "</font>";
                }
            } else {
                // DREAAM development (mSpec template only)
                tag += "<font color=" + GuiConfig.SUB_MISSION_TEXT_COLOR_TEMPLATE + ">";
                shortTag += "<font color=" + GuiConfig.SUB_MISSION_TEXT_COLOR_TEMPLATE + ">";
                for (MissionPlanSpecification subMission : subMissions) {
                    tag += subMission.getName() + "<br>";
                    shortTag += CoreHelper.shorten(subMission.getName(), GuiConfig.MAX_STRING_LENGTH) + "<br>";
                }
                tag += "</font>";
                shortTag += "</font>";
            }
        }
        if (GuiConfig.DRAW_TOKENS) {
            String tokenName;
            for (Token token : tokens) {
                tokenName = token.getName();
                tag += "<font color=" + GuiConfig.TOKEN_TEXT_COLOR + ">" + tokenName + "</font><br>";
                shortTag += "<font color=" + GuiConfig.TOKEN_TEXT_COLOR + ">" + CoreHelper.shorten(tokenName, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
        }
        tag += "</html>";
        shortTag += "</html>";
    }

    public void removeReferences() {
        // Remove any edges connected to this place
        ArrayList<OutEdge> inEdgesClone = (ArrayList<OutEdge>) inEdges.clone();
        for (OutEdge inEdge : inEdgesClone) {
            inEdge.removeReferences();
        }
        ArrayList<InEdge> outEdgesClone = (ArrayList<InEdge>) outEdges.clone();
        for (InEdge outEdge : outEdgesClone) {
            outEdge.removeReferences();
        }
    }

    public void addSubMission(MissionPlanSpecification subMSpec) {
//        inputEvents.add(subMSpec);
        if (subMissionStatus == null) {
            subMissionStatus = new HashMap<MissionPlanSpecification, Boolean>();
        }
        subMissionStatus.put(subMSpec, false);
        updateTag();
    }

    public boolean removeSubMission(MissionPlanSpecification subMSpec) {
//        boolean t = inputEvents.remove(e);
//        t = t && inputEventStatus.remove(e);
        boolean t = subMissionStatus.remove(subMSpec);
        updateTag();
        return t;
    }

//    public ArrayList<MissionPlanSpecification> getSubMissions() {
//        return new ArrayList<MissionPlanSpecification>(subMissionStatus.keySet());
//    }
    public boolean getSubMissionStatus(MissionPlanSpecification subMSpec) {
        if (subMissionStatus.containsKey(subMSpec)) {
            return subMissionStatus.get(subMSpec).booleanValue();
        }
        return false;
    }

    public HashMap<MissionPlanSpecification, Boolean> getSubMissionStatus() {
        return (HashMap<MissionPlanSpecification, Boolean>) subMissionStatus.clone();
    }

    public void clearSubMissionStatus() {
        subMissionStatus.clear();
        updateTag();
    }

    public void setSubMissionStatus(MissionPlanSpecification subMSpec, boolean completed) {
        subMissionStatus.put(subMSpec, completed);
        updateTag();
    }

    public void clearSubMissionInstances() {
        if (subMissionStatus != null) {
            subMissionStatus.clear();
        }
        updateTag();
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            outputEvents = new ArrayList<OutputEvent>();
            tokens = new ArrayList<Token>();
            updateTag();
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        }
    }

    @Override
    public String toString() {
        return "Place:" + name;
    }
}
