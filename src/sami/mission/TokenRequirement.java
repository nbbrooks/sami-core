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

        None, Number, All
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
