package sami.uilanguage.fromui;

import java.lang.reflect.Field;
import java.util.Hashtable;
import sami.event.ReflectedEventSpecification;
import sami.uilanguage.MarkupComponent;
import sami.uilanguage.toui.CreationMessage;
import sami.uilanguage.toui.SelectionMessage;

/**
 *
 * @author nbb
 */
public interface FromUiMessageGeneratorInt {

    /**
     * Used to create FromUiMessage for a creation message used to get values for fields within multiple event specs
     * @param creationMessage
     * @param eventSpecToComponentTable
     * @return 
     */
    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<ReflectedEventSpecification, Hashtable<Field, MarkupComponent>> eventSpecToComponentTable);

    /**
     * Used to create FromUiMessage for a creation message used to get values for fields from a single event spec
     * @param creationMessage
     * @param fieldToComponentTable
     * @param erasureThrowaway not used
     * @return 
     */
    //@todo could roll this into multiple event spec definition?
    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<Field, MarkupComponent> fieldToComponentTable, int erasureThrowaway);

    /**
     * Used to create FromUiMessage for a creation messaged used to get new values for local/global variables
     * @param creationMessage
     * @param variableNameToComponentTable
     * @param erasureThrowaway1 not used
     * @param erasureThrowaway2 not used
     * @return 
     */
    public FromUiMessage getFromUiMessage(CreationMessage creationMessage, Hashtable<String, MarkupComponent> variableNameToComponentTable, int erasureThrowaway1, int erasureThrowaway2);

    /**
     * Used to create FromUiMessage for a selection message where the object is selected at runtime
     * @param selectionMessage
     * @param option
     * @return 
     */
    public FromUiMessage getFromUiMessage(SelectionMessage selectionMessage, Object option);

    /**
     * Used to create FromUiMessage for a selection message where the index to be selected is specified
     *  Used by MI markup to automatically select whatever is the first option
     * @param selectionMessage
     * @param optionIndex
     * @return 
     */
    public FromUiMessage getFromUiMessage(SelectionMessage selectionMessage, int optionIndex);
}
