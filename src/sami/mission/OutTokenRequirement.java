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
            switch (matchQuantity) {
                case Number:
                    ret += quantity + " ";
                    break;
                case LessThan:
                    ret += " < " + quantity + " ";
                    break;
                case GreaterThanEqualTo:
                    ret += " >= " + quantity + " ";
                    break;
                default:
                    ret += matchQuantity + " ";
                    break;
            }
        }
        if (matchCriteria != null) {
            switch (matchCriteria) {
                case Generic:
                    ret += "G ";
                    break;
                case RelevantToken:
                    ret += "RT ";
                    break;
                case SpecificTask:
                    ret += specificTaskName + " ";
                    break;
                case SubMissionToken:
                    ret += "SMT ";
                    break;
                default:
                    ret += matchCriteria + " ";
                    break;
            }
        }
        return ret;
    }
}
