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
public class OutEdge extends Edge {

    static final long serialVersionUID = 1L;
    protected Place endPlace;
    protected Transition startTransition;
    protected ArrayList<OutTokenRequirement> tokenRequirements = new ArrayList<OutTokenRequirement>();

    public OutEdge(Transition startTransition, Place endPlace, FunctionMode functionMode) {
        this.startTransition = startTransition;
        this.endPlace = endPlace;
        this.functionMode = functionMode;
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

    @Override
    public ArrayList<OutTokenRequirement> getTokenRequirements() {
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
            for (OutTokenRequirement outReq : tokenRequirements) {
                tag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + outReq.toString() + "</font><br>";
                shortTag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + shorten(outReq.toString(), GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
            tag += "</html>";
            shortTag += "</html>";
        }
    }

    public void prepareForRemoval() {
        startTransition.removeOutPlace(endPlace);
        startTransition.removeOutEdge(this);
        endPlace.removeInTransition(startTransition);
        endPlace.removeInEdge(this);
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
        String ret = "OutEdge";
        if (startTransition.getName() != null && !startTransition.getName().equals("") && endPlace.getName() != null && !endPlace.getName().equals("")) {
            ret += ":" + startTransition.getName() + "\u21e8" + endPlace.getName();
        }
        return ret;
    }
}
