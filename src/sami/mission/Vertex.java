package sami.mission;

import java.util.ArrayList;
import java.util.UUID;
import sami.CoreHelper;
import sami.event.ReflectedEventSpecification;
import sami.gui.GuiConfig;

/**
 *
 * @author pscerri
 */
public abstract class Vertex implements java.io.Serializable {

    public enum FunctionMode {

        Nominal, Recovery, HiddenRecovery, All, Mockup
    };
    static final long serialVersionUID = 5L;
    protected String name = "";
    protected FunctionMode functionMode = null;
    protected GuiConfig.VisibilityMode visibilityMode = GuiConfig.VisibilityMode.Full;
    protected ArrayList<ReflectedEventSpecification> eventSpecs = new ArrayList<ReflectedEventSpecification>();
    protected ArrayList<ReflectedEventSpecification> lockedEventSpecs = new ArrayList<ReflectedEventSpecification>();
    // Unique vertex ID
    protected long vertexId;
    transient protected String tag = "", shortTag = "";
    transient protected boolean beingModified = false;

    public Vertex(String name, FunctionMode functionMode, long vertexId) {
        this.name = name;
        this.functionMode = functionMode;
        this.vertexId = vertexId;
        tag = name;
        shortTag = CoreHelper.shorten(name, GuiConfig.MAX_STRING_LENGTH);
    }

    public FunctionMode getFunctionMode() {
        return functionMode;
    }

    public void setFunctionMode(FunctionMode functionMode) {
        this.functionMode = functionMode;
    }

    public GuiConfig.VisibilityMode getVisibilityMode() {
        return visibilityMode;
    }

    public void setVisibilityMode(GuiConfig.VisibilityMode visibilityMode) {
        this.visibilityMode = visibilityMode;
    }

    public long getVertexId() {
        return vertexId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateTag();
    }

    public String getTag() {
        return getTag(false);
    }

    public String getTag(boolean updateTag) {
        if (updateTag) {
            updateTag();
        }
        return tag;
    }

    public String getShortTag() {
        return getShortTag(false);
    }

    public String getShortTag(boolean update) {
        if (update) {
            updateTag();
        }
        return shortTag;
    }

    public void addEventSpec(ReflectedEventSpecification eventSpec) {
        eventSpecs.add(eventSpec);
        updateTag();
    }

    public void addEventSpec(ReflectedEventSpecification eventSpec, boolean locked) {
        eventSpecs.add(eventSpec);
        if (locked) {
            lockedEventSpecs.add(eventSpec);
        }
        updateTag();
    }

    public ArrayList<ReflectedEventSpecification> getEventSpecs() {
        return eventSpecs;
    }

    public void setEventSpecs(ArrayList<ReflectedEventSpecification> eventSpecs) {
        this.eventSpecs = eventSpecs;
        updateTag();
    }

    public void updateTag() {
        if (this instanceof MockupPlace) {
            ((MockupPlace) this).updateTag();
        } else if (this instanceof MockupTransition) {
            ((MockupTransition) this).updateTag();
        } else if (this instanceof Place) {
            ((Place) this).updateTag();
        } else if (this instanceof Transition) {
            ((Transition) this).updateTag();
        }
    }

    public boolean getBeingModified() {
        return beingModified;
    }

    public void setBeingModified(boolean beingModified) {
        this.beingModified = beingModified;
    }

    public String toString() {
        return "Vertex: " + name;
    }
}
