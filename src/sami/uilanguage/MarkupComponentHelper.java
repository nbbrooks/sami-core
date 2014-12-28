package sami.uilanguage;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.event.ReflectionHelper;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public class MarkupComponentHelper {

    private static final Logger LOGGER = Logger.getLogger(MarkupComponentHelper.class.getName());

    // Reuse a single instance of components and widgets instead of creating them over and over again
    private static Hashtable<Class, MarkupComponent> componentInstances = new Hashtable<Class, MarkupComponent>();
    private static Hashtable<Class, MarkupComponentWidget> widgetInstances = new Hashtable<Class, MarkupComponentWidget>();

    public static int getCreationComponentScore(ArrayList<Class> supportedCreationClasses, Hashtable<Class, ArrayList<Class>> supportedHashtableCreationClasses, ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, Type type, Field field, ArrayList<Markup> markups) {
        int score = -1;

        if (type instanceof Class) {
            Class creationClass = (Class) type;
            if (creationClass == Hashtable.class) {
                if (field != null) {
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) genericType;
                        Type keyType = parameterizedType.getActualTypeArguments()[0];
                        Type valueType = parameterizedType.getActualTypeArguments()[1];

                        if (keyType instanceof Class && valueType instanceof Class) {
                            Class keyClass = (Class) keyType;
                            Class valueClass = (Class) valueType;
                            for (Class hashtableKey : supportedHashtableCreationClasses.keySet()) {
                                if (hashtableKey.isAssignableFrom(keyClass)) {
                                    for (Class hashtableValue : supportedHashtableCreationClasses.get(hashtableKey)) {
                                        if (hashtableValue.isAssignableFrom(valueClass)) {
                                            score = 0;
                                        }
                                    }
                                }
                            }
                            if (score == 0) {
                                for (Class widgetClass : widgetClasses) {
                                    MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                                    score = Math.max(score, widget.getCreationWidgetScore((Type) creationClass, field, markups));
                                }
                            }
                        }

                    }
                }
            } else {
                if (supportedCreationClasses.contains(creationClass)) {
                    score = 0;
                } else {
                    for (Class widgetClass : widgetClasses) {
                        MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                        score = Math.max(score, widget.getCreationWidgetScore((Type) creationClass, field, markups));
                    }
                }
            }

            if (score >= 0) {
                for (Markup markup : markups) {

                    try {
                        ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                        for (String enumFieldName : enumFieldNames) {
                            Field enumField = ReflectionHelper.getField(markup.getClass(), enumFieldName);
                            if (enumField == null) {
                                LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markup.getClass().getSimpleName() + " or any super class");
                                continue;
                            }
                            Enum enumValue = (Enum) enumField.get(markup);
                            if (supportedMarkups.contains(enumValue)) {
                                score++;
                            } else {
                                for (Class widgetClass : widgetClasses) {
                                    MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                                    ArrayList<Markup> singleMarkup = new ArrayList<Markup>();
                                    singleMarkup.add(markup);
                                    int widgetScore = widget.getMarkupScore(singleMarkup);
                                    if (widgetScore > 0) {
                                        score += widgetScore;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NoSuchFieldException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            LOGGER.severe("Passed in type was not a Class: " + type);
        }
        return score;
    }

    public static int getSelectionComponentScore(ArrayList<Class> supportedSelectionClasses, Hashtable<Class, ArrayList<Class>> supportedHashtableSelectionClasses, ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, Type type, Object object, ArrayList<Markup> markups) {
        int score = -1;

        if (type instanceof Class) {
            Class selectionClass = (Class) type;
            if (selectionClass == Hashtable.class) {
                Hashtable hashtable = (Hashtable) object;
                if (!hashtable.isEmpty()) {
                    Class keyClass = null, valueClass = null;
                    for (Object key : hashtable.keySet()) {
                        keyClass = key.getClass();
                        valueClass = hashtable.get(key).getClass();
                        break;
                    }

                    for (Class hashtableKey : supportedHashtableSelectionClasses.keySet()) {
                        if (hashtableKey.isAssignableFrom(keyClass)) {
                            for (Class hashtableValue : supportedHashtableSelectionClasses.get(hashtableKey)) {
                                if (hashtableValue.isAssignableFrom(valueClass)) {
                                    score = 0;
                                }
                            }
                        }
                    }
                    if (score == 0) {
                        for (Class widgetClass : widgetClasses) {
                            MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                            score = Math.max(score, widget.getSelectionWidgetScore((Type) selectionClass, object, markups));
                        }
                    }
                }
            } else {
                if (supportedSelectionClasses.contains(selectionClass)) {
                    score = 0;
                } else {
                    for (Class widgetClass : widgetClasses) {
                        MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                        score = Math.max(score, widget.getSelectionWidgetScore((Type) selectionClass, object, markups));
                    }
                }
            }

            if (score >= 0) {
                for (Markup markup : markups) {
                    try {
                        ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                        for (String enumFieldName : enumFieldNames) {
                            Field enumField = ReflectionHelper.getField(markup.getClass(), enumFieldName);
                            if (enumField == null) {
                                LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markup.getClass().getSimpleName() + " or any super class");
                                continue;
                            }
                            Enum enumValue = (Enum) enumField.get(markup);
                            if (supportedMarkups.contains(enumValue)) {
                                score++;
                            } else {
                                for (Class widgetClass : widgetClasses) {
                                    MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                                    ArrayList<Markup> singleMarkup = new ArrayList<Markup>();
                                    singleMarkup.add(markup);
                                    int widgetScore = widget.getMarkupScore(singleMarkup);
                                    if (widgetScore > 0) {
                                        score += widgetScore;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NoSuchFieldException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            LOGGER.severe("Passed in type was not a Class: " + type);
        }
        return score;
    }

    public static int getMarkupComponentScore(ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, ArrayList<Markup> markups) {
        int score = 0;
        try {
            for (Markup markup : markups) {
                ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                for (String enumFieldName : enumFieldNames) {
                    Field enumField = ReflectionHelper.getField(markup.getClass(), enumFieldName);
                    if (enumField == null) {
                        LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markup.getClass().getSimpleName() + " or any super class");
                        continue;
                    }
                    Enum enumValue = (Enum) enumField.get(markup);
                    if (supportedMarkups.contains(enumValue)) {
                        score++;
                    } else {
                        for (Class widgetClass : widgetClasses) {
                            MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                            ArrayList<Markup> singleMarkup = new ArrayList<Markup>();
                            singleMarkup.add(markup);
                            int widgetScore = widget.getMarkupScore(singleMarkup);
                            if (widgetScore > 0) {
                                score += widgetScore;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return score;
    }

    public static int getCreationWidgetScore(ArrayList<Class> supportedCreationClasses, Hashtable<Class, ArrayList<Class>> supportedHashtableCreationClasses, ArrayList<Enum> supportedMarkups, Type type, Field field, ArrayList<Markup> markups) {
        int score = -1;

        if (type instanceof Class) {
            Class creationClass = (Class) type;
            if (creationClass == Hashtable.class) {
                if (field != null) {
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) genericType;
                        Type keyType = parameterizedType.getActualTypeArguments()[0];
                        Type valueType = parameterizedType.getActualTypeArguments()[1];

                        if (keyType instanceof Class && valueType instanceof Class) {
                            Class keyClass = (Class) keyType;
                            Class valueClass = (Class) valueType;
                            for (Class hashtableKey : supportedHashtableCreationClasses.keySet()) {
                                if (hashtableKey.isAssignableFrom(keyClass)) {
                                    for (Class hashtableValue : supportedHashtableCreationClasses.get(hashtableKey)) {
                                        if (hashtableValue.isAssignableFrom(valueClass)) {
                                            score = 0;
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            } else {
                if (supportedCreationClasses.contains(creationClass)) {
                    score = 0;
                }
            }

            if (score >= 0) {
                for (Markup markup : markups) {

                    try {
                        ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                        for (String enumFieldName : enumFieldNames) {
                            Field enumField = ReflectionHelper.getField(markup.getClass(), enumFieldName);
                            if (enumField == null) {
                                LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markup.getClass().getSimpleName() + " or any super class");
                                continue;
                            }
                            Enum enumValue = (Enum) enumField.get(markup);
                            if (supportedMarkups.contains(enumValue)) {
                                score++;
                            }
                        }
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NoSuchFieldException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            LOGGER.severe("Passed in type was not a Class: " + type);
        }
        return score;
    }

    public static int getSelectionWidgetScore(ArrayList<Class> supportedSelectionClasses, Hashtable<Class, ArrayList<Class>> supportedHashtableSelectionClasses, ArrayList<Enum> supportedMarkups, Type type, Object object, ArrayList<Markup> markups) {
        int score = -1;

        if (type instanceof Class) {
            Class selectionClass = (Class) type;
            if (selectionClass == Hashtable.class) {
                Hashtable hashtable = (Hashtable) object;
                if (!hashtable.isEmpty()) {
                    Class keyClass = null, valueClass = null;
                    for (Object key : hashtable.keySet()) {
                        keyClass = key.getClass();
                        valueClass = hashtable.get(key).getClass();
                        break;
                    }

                    for (Class hashtableKey : supportedHashtableSelectionClasses.keySet()) {
                        if (hashtableKey.isAssignableFrom(keyClass)) {
                            for (Class hashtableValue : supportedHashtableSelectionClasses.get(hashtableKey)) {
                                if (hashtableValue.isAssignableFrom(valueClass)) {
                                    score = 0;
                                }
                            }
                        }
                    }
                }
            } else {
                if (supportedSelectionClasses.contains(selectionClass)) {
                    score = 0;
                }
            }

            if (score >= 0) {
                for (Markup markup : markups) {

                    try {
                        ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                        for (String enumFieldName : enumFieldNames) {
                            Field enumField = ReflectionHelper.getField(markup.getClass(), enumFieldName);
                            if (enumField == null) {
                                LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markup.getClass().getSimpleName() + " or any super class");
                                continue;
                            }
                            Enum enumValue = (Enum) enumField.get(markup);
                            if (supportedMarkups.contains(enumValue)) {
                                score++;
                            }
                        }
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NoSuchFieldException ex) {
                        Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            LOGGER.severe("Passed in type was not a Class: " + type);
        }
        return score;
    }

    public static int getMarkupWidgetScore(ArrayList<Enum> supportedMarkups, ArrayList<Markup> markups) {
        int score = 0;
        try {
            for (Markup markup : markups) {
                ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                for (String enumFieldName : enumFieldNames) {
                    Field enumField = ReflectionHelper.getField(markup.getClass(), enumFieldName);
                    if (enumField == null) {
                        LOGGER.severe("Could not find field \"" + enumFieldName + "\" in class " + markup.getClass().getSimpleName() + " or any super class");
                        continue;
                    }
                    Enum enumValue = (Enum) enumField.get(markup);
                    if (supportedMarkups.contains(enumValue)) {
                        score++;
                        break;
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return score;
    }

    private static MarkupComponent getComponentInstance(Class componentClass) {
        MarkupComponent component = null;
        if (componentInstances.containsKey(componentClass)) {
            component = componentInstances.get(componentClass);
        } else {
            try {
                Object componentInstance = componentClass.newInstance();
                component = (MarkupComponent) componentInstance;
                componentInstances.put(componentClass, component);
            } catch (InstantiationException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return component;
    }

    private static MarkupComponentWidget getWidgetInstance(Class widgetClass) {
        MarkupComponentWidget widget = null;
        if (widgetInstances.containsKey(widgetClass)) {
            widget = widgetInstances.get(widgetClass);
        } else {
            try {
                Object widgetInstance = widgetClass.newInstance();
                widget = (MarkupComponentWidget) widgetInstance;
                widgetInstances.put(widgetClass, widget);
            } catch (InstantiationException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return widget;
    }
}
