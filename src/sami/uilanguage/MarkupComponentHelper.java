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

    public static int getCreationComponentScore(ArrayList<Class> supportedCreationClasses, ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, Type type, ArrayList<Markup> markups) {
        // First check if we are dealing with a Hashtable
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class && Hashtable.class.isAssignableFrom((Class) pt.getRawType())) {
                // This is a hashtable, recurse on the Hashtable's value
                Type hashtableValueType = pt.getActualTypeArguments()[1];
                return getCreationComponentScore(supportedCreationClasses, supportedMarkups, widgetClasses, hashtableValueType, markups);
            } else {
                LOGGER.severe("Passed in ParameterizedType was not of a supported raw type: " + pt);
                return -1;
            }
        } else if (type instanceof Class) {
            Class creationClass = (Class) type;
            int score = -1;
            try {
                if (supportedCreationClasses.contains(creationClass)) {
                    score = 0;
                } else {
                    for (Class widgetClass : widgetClasses) {
                        MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                        score = Math.max(score, widget.getCreationWidgetScore((Type) creationClass, markups));
                    }
                }
                if (score >= 0) {
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
                }
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return score;
        } else {
            LOGGER.severe("Passed in type was not a ParameterizedType or Class: " + type);
            return -1;
        }
    }

    public static int getSelectionComponentScore(ArrayList<Class> supportedSelectionClasses, ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, Type type, ArrayList<Markup> markups) {
        // First check if we are dealing with a Hashtable
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class && Hashtable.class.isAssignableFrom((Class) pt.getRawType())) {
                // This is a hashtable, recurse on the Hashtable's value
                Type hashtableValueType = pt.getActualTypeArguments()[1];
                return getSelectionComponentScore(supportedSelectionClasses, supportedMarkups, widgetClasses, hashtableValueType, markups);
            } else {
                LOGGER.severe("Passed in ParameterizedType was not of a supported raw type: " + pt);
                return -1;
            }
        } else if (type instanceof Class) {
            Class selectionClass = (Class) type;
            int score = -1;
            try {
                if (supportedSelectionClasses.contains(selectionClass)) {
                    score = 0;
                } else {
                    for (Class widgetClass : widgetClasses) {
                        MarkupComponentWidget widget = getWidgetInstance(widgetClass);
                        score = Math.max(score, widget.getSelectionWidgetScore((Type) selectionClass, markups));
                    }
                }
                if (score >= 0) {
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
                }
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return score;
        } else {
            LOGGER.severe("Passed in type was not a ParameterizedType or Class: " + type);
            return -1;
        }
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

    public static int getCreationWidgetScore(ArrayList<Class> supportedCreationClasses, ArrayList<Enum> supportedMarkups, Type type, ArrayList<Markup> markups) {
        // First check if we are dealing with a Hashtable
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class && Hashtable.class.isAssignableFrom((Class) pt.getRawType())) {
                // This is a hashtable, recurse on the Hashtable's value
                Type hashtableValueType = pt.getActualTypeArguments()[1];
                return getCreationWidgetScore(supportedCreationClasses, supportedMarkups, hashtableValueType, markups);
            } else {
                LOGGER.severe("Passed in ParameterizedType was not of a supported raw type: " + pt);
                return -1;
            }
        } else if (type instanceof Class) {
            Class creationClass = (Class) type;
            int score = -1;
            try {
                if (supportedCreationClasses.contains(creationClass)) {
                    score = 0;
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
                }
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return score;
        } else {
            LOGGER.severe("Passed in type was not a ParameterizedType or Class: " + type);
            return -1;
        }
    }

    public static int getSelectionWidgetScore(ArrayList<Class> supportedSelectionClasses, ArrayList<Enum> supportedMarkups, Type type, ArrayList<Markup> markups) {
        // First check if we are dealing with a Hashtable
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() instanceof Class && Hashtable.class.isAssignableFrom((Class) pt.getRawType())) {
                // This is a hashtable, recurse on the Hashtable's value
                Type hashtableValueType = pt.getActualTypeArguments()[1];
                return getSelectionWidgetScore(supportedSelectionClasses, supportedMarkups, hashtableValueType, markups);
            } else {
                LOGGER.severe("Passed in ParameterizedType was not of a supported raw type: " + pt);
                return -1;
            }
        } else if (type instanceof Class) {
            Class selectionClass = (Class) type;
            int score = -1;
            try {
                if (supportedSelectionClasses.contains(selectionClass)) {
                    score = 0;
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
                }
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            return score;
        } else {
            LOGGER.severe("Passed in type was not a ParameterizedType or Class: " + type);
            return -1;
        }
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
