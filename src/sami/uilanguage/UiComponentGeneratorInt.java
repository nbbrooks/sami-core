package sami.uilanguage;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public interface UiComponentGeneratorInt {

    public enum InteractionType {

        CREATE, SELECT
    };

    public MarkupComponent getCreationComponent(Type type, ArrayList<Markup> markupList);

    public MarkupComponent getSelectionComponent(Type type, Object value, ArrayList<Markup> markupList);

    public Object getComponentValue(MarkupComponent component, Class componentClass);

    public boolean setComponentValue(MarkupComponent component, Object value);

    public ArrayList<Class> getCreationClasses();
}
