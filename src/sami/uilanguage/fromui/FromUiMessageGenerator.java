package sami.uilanguage.fromui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Logger;
import sami.config.DomainConfigManager;

/**
 *
 * @author nbb
 */
public class FromUiMessageGenerator {

    private final static Logger LOGGER = Logger.getLogger(FromUiMessageGenerator.class.getName());
    private static FromUiMessageGeneratorInt instance = null;

    public static synchronized FromUiMessageGeneratorInt getInstance() {
        if (instance == null) {
            instance = createInstance();
        }
        return instance;
    }

    private static FromUiMessageGeneratorInt createInstance() {
        try {
            ArrayList<String> list = (ArrayList<String>) DomainConfigManager.getInstance().getDomainConfiguration().fromUiMessageGeneratorList.clone();
            for (String className : list) {
                Class uiClass = Class.forName(className);
                Method factoryMethod = uiClass.getDeclaredMethod("getInstance");
                Object singleton = factoryMethod.invoke(null, null);
                if (singleton instanceof FromUiMessageGeneratorInt) {
                    return (FromUiMessageGeneratorInt) singleton;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
            System.out.println(ite.getCause());
        }
        LOGGER.severe("Failed to create instance of FromUiMessageGeneratorInterface");
        return null;
    }
}
