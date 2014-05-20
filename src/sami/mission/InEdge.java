package sami.mission;

import java.io.IOException;
import sami.gui.GuiConfig;
import sami.mission.Vertex.FunctionMode;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public InEdge(Place startPlace, Transition endTransition, FunctionMode functionMode) {
        this.startPlace = startPlace;
        this.endTransition = endTransition;
        this.functionMode = functionMode;
        updateTag();
    }

    public void addTokenRequirement(InTokenRequirement requirement) {
        tokenRequirements.add(requirement);
        updateTag();
    }

    @Override
    public ArrayList<InTokenRequirement> getTokenRequirements() {
        return tokenRequirements;
    }

    @Override
    public void clearTokenRequirements() {
        tokenRequirements.clear();
        updateTag();
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
                shortTag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + shorten(inReq.toString(), GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
            tag += "</html>";
            shortTag += "</html>";
        }
    }

    @Override
    public void prepareForRemoval() {
        startPlace.removeOutTransition(endTransition);
        startPlace.removeOutEdge(this);
        endTransition.removeInPlace(startPlace);
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
            updateTag();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
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
