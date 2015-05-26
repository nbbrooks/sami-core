package sami.mission;

import sami.gui.GuiConfig;
import sami.mission.Vertex.FunctionMode;
import java.util.ArrayList;
import sami.CoreHelper;

/**
 *
 * @author nbb
 */
public class MockupInEdge extends InEdge {

    static final long serialVersionUID = 0L;
    protected ArrayList<String> mockupTokenRequirements = new ArrayList<String>();
    protected transient boolean isHighlighted = false;

    public MockupInEdge(MockupPlace startPlace, MockupTransition endTransition, long vertexId) {
        super(startPlace, endTransition, FunctionMode.Mockup, vertexId);
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
        String ret = "MockupInEdge";
        if (startPlace.getName() != null && !startPlace.getName().equals("") && endTransition.getName() != null && !endTransition.getName().equals("")) {
            ret += ":" + startPlace.getName() + "\u21e8" + endTransition.getName();
        }
        return ret;
    }
}
