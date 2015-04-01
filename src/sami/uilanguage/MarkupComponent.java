package sami.uilanguage;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import javax.swing.JComponent;
import sami.engine.PlanManager;
import sami.markup.Markup;
import sami.mission.MissionPlanSpecification;

/**
 *
 * @author nbb
 */
public interface MarkupComponent {

    public abstract int getCreationComponentScore(Type type, Field field, ArrayList<Markup> markups);

    public abstract int getSelectionComponentScore(Type type, Object object, ArrayList<Markup> markups);

    public abstract int getMarkupScore(ArrayList<Markup> markups);

    public abstract MarkupComponent useCreationComponent(Type type, Field field, ArrayList<Markup> markups, MissionPlanSpecification mSpecScope, PlanManager pmScope);

    public abstract MarkupComponent useSelectionComponent(Object selectionObject, ArrayList<Markup> markups, MissionPlanSpecification mSpecScope, PlanManager pmScope);

    public JComponent getComponent();

    public Object getComponentValue(Class componentClass);

    public boolean setComponentValue(Object value);

    public abstract void handleMarkups(ArrayList<Markup> markups, MarkupManager manager);

    public abstract void disableMarkup(Markup markup);

    public abstract ArrayList<Class> getSupportedCreationClasses();
}
