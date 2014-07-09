package sami.uilanguage;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import javax.swing.JComponent;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public interface MarkupComponent {

    public abstract int getCreationComponentScore(Type type, ArrayList<Markup> markups);

    public abstract int getSelectionComponentScore(Type type, ArrayList<Markup> markups);

    public abstract int getMarkupScore(ArrayList<Markup> markups);

    public abstract MarkupComponent useCreationComponent(Type type, ArrayList<Markup> markups);

    public abstract MarkupComponent useSelectionComponent(Object selectionObject, ArrayList<Markup> markups);

    public JComponent getComponent();

    public Object getComponentValue(Class componentClass);

    public boolean setComponentValue(Object value);

    public abstract void handleMarkups(ArrayList<Markup> markups, MarkupManager manager);

    public abstract void disableMarkup(Markup markup);

    public abstract ArrayList<Class> getSupportedCreationClasses();
}
