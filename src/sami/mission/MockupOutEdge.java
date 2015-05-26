package sami.mission;

import sami.gui.GuiConfig;
import sami.mission.Vertex.FunctionMode;
import java.util.ArrayList;
import sami.CoreHelper;

/**
 *
 * @author nbb
 */
public class MockupOutEdge extends OutEdge {

    static final long serialVersionUID = 0L;
    protected ArrayList<String> mockupTokenRequirements = new ArrayList<String>();
    protected transient boolean isHighlighted = false;

    public MockupOutEdge(MockupTransition startTransition, MockupPlace endPlace, long vertexId) {
        super(startTransition, endPlace, FunctionMode.Mockup, vertexId);
    }

    public ArrayList<String> getMockupTokenRequirements() {
        return mockupTokenRequirements;
    }

    public void setMockupTokenRequirements(ArrayList<String> mockupTokenRequirements) {
        this.mockupTokenRequirements = mockupTokenRequirements;
        updateTag();
    }
    
    public boolean getIsHighlighted() {
        return isHighlighted;
    }
    
    public void setIsHighlighted(boolean isHighlighted) {
        this.isHighlighted =  isHighlighted;
    }

    @Override
    public void updateTag() {
        tag = "";
        shortTag = "";
        if (mockupTokenRequirements != null && !mockupTokenRequirements.isEmpty()) {
            tag += "<html>";
            shortTag += "<html>";
            for (String tokenReq : mockupTokenRequirements) {
                tag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + tokenReq + "</font><br>";
                shortTag += "<font color=" + GuiConfig.TOKEN_REQ_TEXT_COLOR + ">" + CoreHelper.shorten(tokenReq, GuiConfig.MAX_STRING_LENGTH) + "</font><br>";
            }
            tag += "</html>";
            shortTag += "</html>";
        }
    }

//    private void readObject(ObjectInputStream ois) {
//        try {
//            ois.defaultReadObject();
//            updateTag();
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        } catch (ClassNotFoundException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    private void writeObject(ObjectOutputStream os) {
//        try {
//            os.defaultWriteObject();
//        } catch (IOException ex) {
//            Logger.getLogger(ReflectedEventSpecification.class.getName()).log(Level.SEVERE, null, ex);
//            ex.printStackTrace();
//        }
//    }
    @Override
    public String toString() {
        String ret = "MockupOutEdge";
        if (startTransition.getName() != null && !startTransition.getName().equals("") && endPlace.getName() != null && !endPlace.getName().equals("")) {
            ret += ":" + startTransition.getName() + "\u21e8" + endPlace.getName();
        }
        return ret;
    }
}
