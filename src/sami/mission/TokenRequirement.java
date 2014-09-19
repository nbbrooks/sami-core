package sami.mission;

/**
 *
 * @author nbb
 */
public class TokenRequirement implements java.io.Serializable {

    static final long serialVersionUID = 0L;

    public enum MatchCriteria {

        SpecificTask,
        None,
        RelevantToken,
        Generic,
        AnyTask,
        AnyProxy,
        AnyToken,
        SubMissionToken
    };

    public enum MatchQuantity {

        None, Number, All, LessThan, GreaterThanEqualTo
    }

    public enum MatchAction {

        Take, Consume, Add
    }

    protected MatchCriteria matchCriteria;
    protected MatchQuantity matchQuantity;
    protected int quantity;
    protected String specificTaskName;

    public TokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity) {
        this.matchCriteria = matchCriteria;
        this.matchQuantity = matchQuantity;
    }

    public TokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, int quantity) {
        this(matchCriteria, matchQuantity);
        this.quantity = quantity;
    }

    public TokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, String specificTaskName) {
        this(matchCriteria, matchQuantity);
        this.specificTaskName = specificTaskName;
    }

    public TokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, int quantity, String specificTaskName) {
        this(matchCriteria, matchQuantity);
        this.quantity = quantity;
        this.specificTaskName = specificTaskName;
    }

    public MatchCriteria getMatchCriteria() {
        return matchCriteria;
    }

    public MatchQuantity getMatchQuantity() {
        return matchQuantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getTaskName() {
        return specificTaskName;
    }

    @Override
    public String toString() {
        String ret = "";
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
