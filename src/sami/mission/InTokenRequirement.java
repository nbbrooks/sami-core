package sami.mission;

/**
 *
 * @author nbb
 */
public class InTokenRequirement extends TokenRequirement {

    static final long serialVersionUID = 0L;

    public InTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity) {
        super(matchCriteria, matchQuantity);
    }

    public InTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, int quantity) {
        super(matchCriteria, matchQuantity, quantity);
    }

    public InTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, String specificTaskName) {
        super(matchCriteria, matchQuantity, specificTaskName);
    }

    public InTokenRequirement(MatchCriteria matchCriteria, MatchQuantity matchQuantity, int quantity, String specificTaskName) {
        super(matchCriteria, matchQuantity, quantity, specificTaskName);
    }
}
