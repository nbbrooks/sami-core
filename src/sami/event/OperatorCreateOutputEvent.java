package sami.event;

/**
 * Output event classes should extend this instead of OutputEvent if they will
 * be used to have the operator create a value at run-time when the place
 * containing this event is entered, and be saved to a variable with the
 * corresponding InputEvent
 *
 * @author nbb
 */
public abstract class OperatorCreateOutputEvent extends OutputEvent {
    /*
     Classes extending OperatorOutputEvent should:
     - NOT add anything to fieldNames 
     - NOT add anything to fieldNameToDescription
     - implement getInputEventClass(), which should add the desired objects to be defined to variableNames and variableNameToDescription 
     */

    /**
     * Get the corresponding InputEvent for this class
     *
     * @return The InputEvent extending class corresponding to this event
     */
    public abstract Class getInputEventClass();
}
