package sami.mission;

/**
 *
 * @author nbb
 */
public class OutTokenRequirement extends TokenRequirement {

    static final long serialVersionUID = 0L;
    protected MatchAction matchAction;

    public OutTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, MatchAction matchAction) {
        super(matchCriteria, matchQuantity);
        this.matchAction = matchAction;
    }

    public OutTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, MatchAction matchAction, int quantity) {
        super(matchCriteria, matchQuantity, quantity);
        this.matchAction = matchAction;
    }

    public OutTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, MatchAction matchAction, String specificTaskName) {
        super(matchCriteria, matchQuantity, specificTaskName);
        this.matchAction = matchAction;
    }

    public OutTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, MatchAction matchAction, int quantity, String specificTaskName) {
        super(matchCriteria, matchQuantity, quantity, specificTaskName);
        this.matchAction = matchAction;
    }

    public MatchAction getMatchAction() {
        return matchAction;
    }

    @Override
    public String toString() {
        String ret = "";
        if (matchAction != null) {
            ret += matchAction + " ";
        }
        if (matchQuantity != null) {
            if (matchQuantity == MatchQuantity.Number) {
                ret += quantity + " ";
            } else {
                ret += matchQuantity + " ";
            }
        }
        if (matchCriteria != null) {
            if (matchCriteria == MatchCriteria.SpecificTask) {
                ret += specificTaskName + " ";
            } else {
                ret += matchCriteria + " ";
            }
        }
        return ret;
    }
}
