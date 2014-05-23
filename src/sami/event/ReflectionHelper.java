package sami.event;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 *
 * @author nbb
 */
public class ReflectionHelper {

    private final static Logger LOGGER = Logger.getLogger(ReflectionHelper.class.getName());

    public static Field getField(Class childClass, String fieldName) {
        try {
            Field field = childClass.getField(fieldName);
            return field;
        } catch (NoSuchFieldException ex) {
            if (childClass.getSuperclass() != null) {
                return getField(childClass.getSuperclass(), fieldName);
            } else {
                return null;
            }
        }
    }
}
