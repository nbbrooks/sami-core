package sami.mission;

import java.io.IOException;
import sami.gui.GuiConfig;
import sami.mission.Vertex.FunctionMode;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.CoreHelper;
import sami.event.ReflectedEventSpecification;

/**
 *
 * @author nbb
 */
public class OutEdge extends Edge {

    static final long serialVersionUID = 1L;
    protected Place endPlace;
    protected Transition startTransition;
    protected ArrayList<OutTokenRequirement> tokenRequirements = new ArrayList<OutTokenRequirement>();
    protected ArrayList<OutTokenRequirement> lockedTokenRequirements = new ArrayList<OutTokenRequirement>();

    public OutEdge(Transition startTransition, Place endPlace, FunctionMode functionMode, long edgeId) {
        this.startTransition = startTransition;
        this.endPlace = endPlace;
        this.functionMode = functionMode;
        this.edgeId = edgeId;
        // Add place and transition's references to each other
        startTransition.addOutPlace(endPlace);
        endPlace.addInTransition(startTransition);
        // Add references to this edge in the vertices
        startTransition.addOutEdge(this);
        endPlace.addInEdge(this);
        updateTag();
    }

    @Override
    public Place getEnd() {
        return endPlace;
    }

    public void setEnd(Place endPlace) {
        this.endPlace = endPlace;
    }

    @Override
    public Transition getStart() {
        return startTransition;
    }

    public void setStart(Transition startTransition) {
        this.startTransition = startTransition;
    }

    public void addTokenRequirement(OutTokenRequirement requirement) {
        tokenRequirements.add(requirement);
        updateTag();
    }

    public void addTokenRequirement(OutTokenRequirement requirement, boolean locked) {
        tokenRequirements.add(requirement);
        if (locked) {
            lockedTokenRequirements.add(requirement);
        }
        updateTag();
    }

    @Override
    public ArrayList<OutTokenRequirement> getTokenRequirements() {
        return tokenRequirements;
    }

    @Override
    public ArrayList<OutTokenRequirement> getLockedTokenRequirements() {
        return lockedTokenRequirements;
    }

    @Override
    public void clearTokenRequirements() {
        tokenRequirements.clear();
        lockedTokenRequirements.clear();
        updateTag();
    }

    public long getEdgeId() {
        return edgeId;
    }

    @Override
    public void updateTag() {
        tag = "";
        shortTag = "";
        if (GuiConfig.DRAW_TOKEN_REQS && tokenRequirements.size() > 0) {
            tag += "<html>";
            shortTag += "<html>";
            for (OutTokenRequirement outReq : tokenRequirements) {
                tag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + outReq.toString() + "</font><br>";
                shortTag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + CoreHelper.shorten(outReq.toString(), GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
            tag += "</html>";
            shortTag += "</html>";
        }
    }

    @Override
    public void removeReferences() {
        // Remove place and transition's references to each other
        startTransition.removeOutPlace(endPlace);
        endPlace.removeInTransition(startTransition);
        // Remove references to this edge in the vertices
        startTransition.removeOutEdge(this);
        endPlace.removeInEdge(this);
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            if (lockedTokenRequirements == null) {
                lockedTokenRequirements = new ArrayList<OutTokenRequirement>();
            }
            updateTag();
        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
        }
    }

    private void writeObject(ObjectOutputStream os) {
        try {
            os.defaultWriteObject();
        } catch (IOException ex) {
            Logger.getLogger(ReflectedEventSpecification.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    @Override
    public String toString() {
        String ret = "OutEdge";
        if (startTransition.getName() != null && !startTransition.getName().equals("") && endPlace.getName() != null && !endPlace.getName().equals("")) {
            ret += ":" + startTransition.getName() + "\u21e8" + endPlace.getName();
        }
        return ret;
    }
}
