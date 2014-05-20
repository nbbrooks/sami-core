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
    transient String tag = "", shortTag = "";

    public abstract void updateTag();

    public abstract void prepareForRemoval();

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

    public String shorten(String full, int maxLength) {
        String reduced = "";
        int upperCount = 0;
        for (char c : full.toCharArray()) {
            if (Character.isUpperCase(c) || c == '.') {
                upperCount++;
            }
        }
        int charPerUpper = maxLength / Math.max(1, upperCount); // prevent divide by 0
        int lowerCaseAfterUpperCount = 0;
        for (int i = 0; i < full.length(); i++) {
            if (Character.isUpperCase(full.charAt(i)) || full.charAt(i) == '.') {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount = 0;
            } else if (lowerCaseAfterUpperCount < charPerUpper) {
                reduced += full.charAt(i);
                lowerCaseAfterUpperCount++;
            }
        }
        return reduced;
    }
}
