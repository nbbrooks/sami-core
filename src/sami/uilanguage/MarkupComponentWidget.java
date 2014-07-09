package sami.uilanguage;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public interface MarkupComponentWidget {

    public abstract int getCreationWidgetScore(Type type, ArrayList<Markup> markups);

    public abstract int getSelectionWidgetScore(Type type, ArrayList<Markup> markups);

    public abstract int getMarkupScore(ArrayList<Markup> markups);

    public abstract MarkupComponentWidget addCreationWidget(MarkupComponent component, Type type, ArrayList<Markup> markups);

    public abstract MarkupComponentWidget addSelectionWidget(MarkupComponent component, Object selectionObject, ArrayList<Markup> markups);

    public Object getComponentValue(Field field);

    public boolean setComponentValue(Object value);

    public abstract void handleMarkups(ArrayList<Markup> markups, MarkupManager manager);

    public abstract void disableMarkup(Markup markup);

    public abstract ArrayList<Class> getSupportedCreationClasses();
}
