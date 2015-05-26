package sami.mission;

import java.awt.Rectangle;
import java.awt.Shape;
import sami.gui.GuiConfig;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;
import sami.CoreHelper;

/**
 *
 * @author pscerri
 */
public class MockupTransition extends Transition {

    public enum MockupIeStatus {

        INCOMPLETE, COMPLETE, INACTIVE
    };
    private static final Logger LOGGER = Logger.getLogger(Place.class.getName());
    static final long serialVersionUID = 0L;
    protected Hashtable<String, MockupIeStatus> mockupInputEventStatus = new Hashtable<String, MockupIeStatus>();
    protected Hashtable<String, ArrayList<String>> mockupInputEventMarkups = new Hashtable<String, ArrayList<String>>();
    protected transient boolean isHighlighted = false;

    public MockupTransition(String name, long vertexId) {
        super(name, FunctionMode.Mockup, vertexId);
    }

    public Hashtable<String, MockupIeStatus> getMockupInputEventStatus() {
        return mockupInputEventStatus;
    }

    public void setMockupInputEventStatus(Hashtable<String, MockupIeStatus> mockupInputEventStatus) {
        this.mockupInputEventStatus = mockupInputEventStatus;
        updateTag();
    }

    public Hashtable<String, ArrayList<String>> getMockupInputEventMarkups() {
        return mockupInputEventMarkups;
    }

    public void setMockupInputEventMarkups(Hashtable<String, ArrayList<String>> mockupInputEventMarkups) {
        this.mockupInputEventMarkups = mockupInputEventMarkups;
        updateTag();
    }
    
    public boolean getIsHighlighted() {
        return isHighlighted;
    }
    
    public void setIsHighlighted(boolean isHighlighted) {
        this.isHighlighted =  isHighlighted;
    }

    @Override
    public Shape getShape() {
        int mult = 2;
        return new Rectangle(-10*mult, -10*mult, 20*mult, 20*mult);
    }

    @Override
    public void updateTag() {
        tag = "<html>";
        shortTag = "<html>";
        // Name
        if (name != null && !name.equals("")) {
            String nameTextColor = isHighlighted ? GuiConfig.SEL_LABEL_TEXT_COLOR : GuiConfig.LABEL_TEXT_COLOR;
            tag += "<font color=" + nameTextColor + ">" + name + "</font><br>";
            shortTag += "<font color=" + nameTextColor + ">" + CoreHelper.shorten(name, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
        }
        // Input events
        if (mockupInputEventStatus != null) {
            for (String ie : mockupInputEventStatus.keySet()) {
                // Run-time execution (instantiated input events)
                String color;
                switch (mockupInputEventStatus.get(ie)) {
                    case COMPLETE:
                        color = GuiConfig.INPUT_EVENT_TEXT_COLOR_COMPLETE;
                        break;
                    case INCOMPLETE:
                        color = GuiConfig.INPUT_EVENT_TEXT_COLOR_INACTIVE;
                        break;
                    case INACTIVE:
                    default:
                        color = GuiConfig.INPUT_EVENT_TEXT_COLOR_INACTIVE;
                        break;
                }
                tag += "<font color=" + color + ">" + ie + "</font><br>";
                shortTag += "<font color=" + color + ">" + CoreHelper.shorten(ie, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                if (mockupInputEventMarkups.containsKey(ie)) {
                    for (String markup : mockupInputEventMarkups.get(ie)) {
                        tag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\t" + markup + "</font><br>";
                        shortTag += "<font color=" + GuiConfig.MARKUP_TEXT_COLOR + ">\t" + CoreHelper.shorten(markup, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
                    }
                }
            }
        }
        tag += "</html>";
        shortTag += "</html>";
    }

    public String toString() {
        return "MockupTransition:" + name;
    }
//    private void readObject(ObjectInputStream ois) {
//        try {
//            ois.defaultReadObject();
//            inputEvents = new ArrayList<InputEvent>();
//            inputEventStatus = new Hashtable<InputEvent, Boolean>();
//            updateTag();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
}
