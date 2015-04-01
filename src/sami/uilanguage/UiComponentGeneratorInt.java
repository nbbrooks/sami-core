package sami.uilanguage;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import sami.engine.PlanManager;
import sami.markup.Markup;
import sami.mission.MissionPlanSpecification;

/**
 *
 * @author nbb
 */
public interface UiComponentGeneratorInt {

    public enum InteractionType {

        CREATE, SELECT
    };

    public MarkupComponent getCreationComponent(Type type, Field field, ArrayList<Markup> markupList, MissionPlanSpecification mSpecScope, PlanManager pmScope);

    public MarkupComponent getSelectionComponent(Type type, Object value, ArrayList<Markup> markupList, MissionPlanSpecification mSpecScope, PlanManager pmScope);

    public Object getComponentValue(MarkupComponent component, Class componentClass);

    public boolean setComponentValue(MarkupComponent component, Object value);

    public ArrayList<Class> getCreationClasses();
}
