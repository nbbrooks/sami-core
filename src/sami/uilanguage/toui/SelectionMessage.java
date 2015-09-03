package sami.uilanguage.toui;

import java.util.List;
import java.util.UUID;

/**
 * Rename from Decision to Selection?
 *
 * @author nbb
 */
public class SelectionMessage extends ToUiMessage {

    final protected boolean allowMultiple;
    final protected boolean allowRejection;
    final protected boolean showOptionsIndividually;
    protected List<?> optionsList;

    public SelectionMessage(UUID relevantOutputEventId, UUID missionId, int priority, boolean allowMultiple, boolean allowRejection, boolean showOptionsIndividually) {
        super(relevantOutputEventId, missionId, priority);
        this.allowMultiple = allowMultiple;
        this.allowRejection = allowRejection;
        this.showOptionsIndividually = showOptionsIndividually;
    }

    public SelectionMessage(UUID relevantOutputEventId, UUID missionId, int priority, boolean allowMultiple, boolean allowRejection, boolean showOptionsIndividually, List<?> optionsList) {
        this(relevantOutputEventId, missionId, priority, allowMultiple, allowRejection, showOptionsIndividually);
        this.optionsList = optionsList;
    }

    public boolean getAllowMultiple() {
        return allowMultiple;
    }

    public boolean getAllowRejection() {
        return allowRejection;
    }

    public boolean getShowEachIndividually() {
        return showOptionsIndividually;
    }

    public List<?> getOptionsList() {
        return optionsList;
    }

    @Override
    public String toString() {
        return "SelectionMessage [" + allowMultiple + ", " + allowRejection + ", " + showOptionsIndividually + ", " + optionsList + "]";
    }
}
