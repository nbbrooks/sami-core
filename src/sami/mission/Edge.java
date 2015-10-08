package sami.mission;

import sami.gui.GuiConfig;
import sami.mission.Vertex.FunctionMode;
import java.util.ArrayList;

/**
 *
 * @author pscerri
 */
public abstract class Edge implements java.io.Serializable {

    static final long serialVersionUID = 5L;
    protected FunctionMode functionMode = null;
    protected GuiConfig.VisibilityMode visibilityMode = GuiConfig.VisibilityMode.Full;
    // Unique vertex ID
    protected long edgeId;
    transient String tag = "", shortTag = "";

    public abstract void updateTag();

    public abstract void removeReferences();

    public abstract Vertex getStart();

    public abstract Vertex getEnd();

    public abstract ArrayList<? extends TokenRequirement> getTokenRequirements();

    public abstract void clearTokenRequirements();

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

    public String getTag() {
        return getTag(false);
    }

    public String getTag(boolean update) {
        if (update) {
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
}
