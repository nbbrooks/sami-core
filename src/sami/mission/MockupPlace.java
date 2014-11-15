package sami.mission;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.gui.GuiConfig;

/**
 *
 * @author pscerri
 */
public class MockupPlace extends Place {

    public enum MockupSubMissionType {

        TEMPLATE, INCOMPLETE, COMPLETE
    };
    private static final Logger LOGGER = Logger.getLogger(MockupPlace.class.getName());
    static final long serialVersionUID = 0L;
    protected Hashtable<String, ArrayList<String>> mockupOutputEventMarkups = new Hashtable<String, ArrayList<String>>();
    protected ArrayList<String> mockupTokens = new ArrayList<String>();
    protected Hashtable<String, MockupSubMissionType> mockupSubMissionType = new Hashtable<String, MockupSubMissionType>();

    public MockupPlace(String name) {
        super(name, FunctionMode.Mockup);
    }

    public Hashtable<String, ArrayList<String>> getMockupOutputEventMarkups() {
        return mockupOutputEventMarkups;
    }

    public void setMockupOutputEventMarkups(Hashtable<String, ArrayList<String>> mockupOutputEventMarkups) {
        this.mockupOutputEventMarkups = mockupOutputEventMarkups;
        updateTag();
    }

    public ArrayList<String> getMockupTokens() {
        return mockupTokens;
    }

    public void setMockupTokens(ArrayList<String> mockupTokens) {
        this.mockupTokens = mockupTokens;
        updateTag();
    }

    public Hashtable<String, MockupSubMissionType> getMockupSubMissionType() {
        return mockupSubMissionType;
    }

    public void setMockupSubMissionType(Hashtable<String, MockupSubMissionType> mockupSubMissionType) {
        this.mockupSubMissionType = mockupSubMissionType;
        updateTag();
    }

    @Override
    public void updateTag() {
        tag = "<html>";
        shortTag = "<html>";
        // Name
        if (name != null && !name.equals("")) {
            tag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + name + "</font><br>";
            shortTag += "<font color=" + GuiConfig.LABEL_TEXT_COLOR + ">" + shorten(name, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
        }
        // Output events
        if (mockupOutputEventMarkups != null) {
            for (String oe : mockupOutputEventMarkups.keySet()) {
                tag += "<font color=" + GuiConfig.OUTPUT_EVENT_TEXT_COLOR + ">" + oe + "</font><br>";
                shortTag += "<font color=" + GuiConfig.OUTPUT_EVENT_TEXT_COLOR + ">" + shorten(oe, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                for (String markupSpec : mockupOutputEventMarkups.get(oe)) {
                    tag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\t" + markupSpec + "</font><br>";
                    shortTag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\t" + shorten(markupSpec, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                }
            }
        }
        // Sub missions
        if (mockupSubMissionType != null && !mockupSubMissionType.isEmpty()) {
            // Run-time execution (mSpec template and spawned instances)
            for (String subMissionName : mockupSubMissionType.keySet()) {
                String color;
                switch (mockupSubMissionType.get(subMissionName)) {
                    case COMPLETE:
                        color = GuiConfig.SUB_MISSION_TEXT_COLOR_COMPLETE;
                        break;
                    case INCOMPLETE:
                        color = GuiConfig.SUB_MISSION_TEXT_COLOR_INCOMPLETE;
                        break;
                    case TEMPLATE:
                    default:
                        color = GuiConfig.SUB_MISSION_TEXT_COLOR_TEMPLATE;
                        break;
                }
                tag += "<font color=" + color + ">";
                shortTag += "<font color=" + color + ">";
                tag += subMissionName + "<br>";
                shortTag += shorten(subMissionName, GuiConfig.MAX_STRING_LENGTH) + "<br>";
                tag += "</font>";
                shortTag += "</font>";
            }
        }
        // Tokens
        if (mockupTokens != null) {
            for (String tokenName : mockupTokens) {
                tag += "<font color=" + GuiConfig.TOKEN_TEXT_COLOR + ">" + tokenName + "</font><br>";
                shortTag += "<font color=" + GuiConfig.TOKEN_TEXT_COLOR + ">" + shorten(tokenName, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
        }
        tag += "</html>";
        shortTag += "</html>";
    }

    @Override
    public String toString() {
        return "Place:" + name;
    }
//    private void readObject(ObjectInputStream ois) {
//        try {
//            ois.defaultReadObject();
//            outputEvents = new ArrayList<OutputEvent>();
//            tokens = new ArrayList<Token>();
//            updateTag();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
}
