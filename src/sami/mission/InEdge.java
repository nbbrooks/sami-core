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
public class InEdge extends Edge {

    static final long serialVersionUID = 1L;
    protected Place startPlace;
    protected Transition endTransition;
    protected ArrayList<InTokenRequirement> tokenRequirements = new ArrayList<InTokenRequirement>();
    protected ArrayList<InTokenRequirement> lockedTokenRequirements = new ArrayList<InTokenRequirement>();

    public InEdge(Place startPlace, Transition endTransition, FunctionMode functionMode, long edgeId) {
        this.startPlace = startPlace;
        this.endTransition = endTransition;
        this.functionMode = functionMode;
        this.edgeId = edgeId;
        // Add place and transition's references to each other
        startPlace.addOutTransition(endTransition);
        endTransition.addInPlace(startPlace);
        // Add references to this edge in the vertices
        startPlace.addOutEdge(this);
        endTransition.addInEdge(this);
        updateTag();
    }

    public void addTokenRequirement(InTokenRequirement requirement) {
        tokenRequirements.add(requirement);
        updateTag();
    }

    public void addTokenRequirement(InTokenRequirement requirement, boolean locked) {
        tokenRequirements.add(requirement);
        if (locked) {
            lockedTokenRequirements.add(requirement);
        }
        updateTag();
    }

    @Override
    public ArrayList<InTokenRequirement> getTokenRequirements() {
        return tokenRequirements;
    }

    @Override
    public ArrayList<InTokenRequirement> getLockedTokenRequirements() {
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
            for (InTokenRequirement inReq : tokenRequirements) {
                tag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + inReq.toString() + "</font><br>";
                shortTag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + CoreHelper.shorten(inReq.toString(), GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
            tag += "</html>";
            shortTag += "</html>";
        }
    }

    @Override
    public void removeReferences() {
        // Remove place and transition's references to each other
        startPlace.removeOutTransition(endTransition);
        endTransition.removeInPlace(startPlace);
        // Remove references to this edge in the vertices
        startPlace.removeOutEdge(this);
        endTransition.removeInEdge(this);
    }

    @Override
    public Place getStart() {
        return startPlace;
    }

    @Override
    public Transition getEnd() {
        return endTransition;
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            if (lockedTokenRequirements == null) {
                lockedTokenRequirements = new ArrayList<InTokenRequirement>();
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
        String ret = "InEdge";
        if (startPlace.getName() != null && !startPlace.getName().equals("") && endTransition.getName() != null && !endTransition.getName().equals("")) {
            ret += ":" + startPlace.getName() + "\u21e8" + endTransition.getName();
        }
        return ret;
    }
}
