package sami.mission;

import sami.event.InputEvent;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.markup.ReflectedMarkupSpecification;

/**
 *
 * @author pscerri
 */
public class Transition extends Vertex {

    private static final Logger LOGGER = Logger.getLogger(Place.class.getName());
    static final long serialVersionUID = 6L;
    protected ArrayList<InEdge> inEdges = new ArrayList<InEdge>();
    protected ArrayList<OutEdge> outEdges = new ArrayList<OutEdge>();
    private ArrayList<Place> inPlaces = new ArrayList<Place>();
    private ArrayList<Place> outPlaces = new ArrayList<Place>();
    transient private ArrayList<InputEvent> inputEvents = new ArrayList<InputEvent>();
    transient private Hashtable<InputEvent, Boolean> inputEventStatus = new Hashtable<InputEvent, Boolean>();

    public Transition(String name, FunctionMode functionMode) {
        super(name, functionMode);
    }

    public void addInEdge(InEdge edge) {
        inEdges.add(edge);
    }

    public boolean removeInEdge(InEdge edge) {
        return inEdges.remove(edge);
    }

    public ArrayList<InEdge> getInEdges() {
        return inEdges;
    }

    public void addOutEdge(OutEdge edge) {
        outEdges.add(edge);
    }

    public boolean removeOutEdge(OutEdge edge) {
        return outEdges.remove(edge);
    }

    public ArrayList<OutEdge> getOutEdges() {
        return outEdges;
    }

    public void addInPlace(Place p) {
        if (inPlaces.contains(p)) {
            LOGGER.severe("Tried to add pre-existing inPlace: " + p);
            return;
        }
        inPlaces.add(p);
    }

    public void removeInPlace(Place p) {
        if (!inPlaces.contains(p)) {
            LOGGER.severe("Tried to remove non-existing inPlace: " + p);
            return;
        }
        inPlaces.remove(p);
    }

    public ArrayList<Place> getInPlaces() {
        return inPlaces;
    }

    public void setInPlaces(ArrayList<Place> inPlaces) {
        this.inPlaces = inPlaces;
    }

    public void addOutPlace(Place p) {
        if (outPlaces.contains(p)) {
            LOGGER.severe("Tried to add pre-existing outPlace: " + p);
            return;
        }
        outPlaces.add(p);
    }

    public void removeOutPlace(Place p) {
        if (!outPlaces.contains(p)) {
            LOGGER.severe("Tried to remove non-existing outPlace: " + p);
            return;
        }
        outPlaces.remove(p);
    }

    public ArrayList<Place> getOutPlaces() {
        return outPlaces;
    }

    public void setOutPlaces(ArrayList<Place> outPlaces) {
        this.outPlaces = outPlaces;
    }

    /**
     * Called when reading in a spec to run a mission, not when creating the
     * mission in the GUI
     *
     * @param e
     */
    public void addInputEvent(InputEvent e) {
        inputEvents.add(e);
        inputEventStatus.put(e, new Boolean(false));
    }

    public boolean removeInputEvent(InputEvent e) {
        boolean t = inputEvents.remove(e);
        t = t && inputEventStatus.remove(e);
        return t;
    }

    public ArrayList<InputEvent> getInputEvents() {
        return inputEvents;
    }

    public boolean getInputEventStatus(InputEvent e) {
        if (inputEventStatus.containsKey(e)) {
            return inputEventStatus.get(e).booleanValue();
        }
        return false;
    }

    public Hashtable<InputEvent, Boolean> getInputEventStatus() {
        return (Hashtable<InputEvent, Boolean>) inputEventStatus.clone();
    }

    public void clearInputEventStatus() {
        inputEventStatus.clear();
    }

    public void setInputEventStatus(InputEvent e, boolean received) {
        inputEventStatus.put(e, new Boolean(received));
    }

    public Shape getShape() {
        return new Rectangle(-10, -10, 20, 20);
    }

    @Override
    public void updateTag() {
        tag = "<html>";
        shortTag = "<html>";
        if (GuiConfig.DRAW_LABELS && name != null && !name.equals("")) {
            tag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + name + "</font><br>";
            shortTag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + shorten(name, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
        }
        if (GuiConfig.DRAW_EVENTS) {
            for (ReflectedEventSpecification eventSpec : eventSpecs) {
                try {
                    Class eventClass = Class.forName(eventSpec.getClassName());
                    String simpleName = eventClass.getSimpleName();
                    if (InputEvent.class.isAssignableFrom(eventClass)) {
                        tag += "<font color=" + GuiConfig.INPUT_EVENT_TEXT_COLOR + ">I: " + simpleName + "</font><br>";
                        shortTag += "<font color=" + GuiConfig.INPUT_EVENT_TEXT_COLOR + ">I: " + shorten(simpleName, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                    } else {
                        continue;
                    }
                    if (GuiConfig.DRAW_MARKUPS) {
                        for (ReflectedMarkupSpecification markupSpec : eventSpec.getMarkupSpecs()) {
                            Class markupClass = Class.forName(markupSpec.getClassName());
                            tag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\tM: " + markupClass.getSimpleName() + "</font><br>";
                            shortTag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\tM:  " + shorten(markupClass.getSimpleName(), GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                        }
                    }
                } catch (ClassNotFoundException cnfe) {
                    cnfe.printStackTrace();
                }
            }
        }
        tag += "</html>";
        shortTag += "</html>";
    }

    public String toString() {
        return "Transition:" + name;
    }

    public void prepareForRemoval() {
        // Remove each edge
        ArrayList<InEdge> inEdgesClone = (ArrayList<InEdge>) inEdges.clone();
        for (InEdge inEdge : inEdgesClone) {
            inEdge.prepareForRemoval();
        }
        ArrayList<OutEdge> outEdgesClone = (ArrayList<OutEdge>) outEdges.clone();
        for (OutEdge outEdge : outEdgesClone) {
            outEdge.prepareForRemoval();
        }
    }

    private void readObject(ObjectInputStream ois) {
        try {
            ois.defaultReadObject();
            inputEvents = new ArrayList<InputEvent>();
            inputEventStatus = new Hashtable<InputEvent, Boolean>();
            updateTag();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
