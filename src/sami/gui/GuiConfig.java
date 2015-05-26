package sami.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

/**
 * Controls how things are drawn in the TaskModelEditor and displayed in the
 * MissionMonitor Later this may switch to a singleton class with values read in
 * from a file and could contain other user-specific preferences
 *
 * @author nbb
 */
public class GuiConfig {


    /*
     * Based on Vertex/Edge's FunctionMode:
     * NOMINAL:
     *  While in nominal mode, hide all recovery mode vertices and edges with at least one connection to a recovery mode vertex
     * RECOVERY:
     *  While in recovery mode, hide all edges connecting two nominal mode vertices
     * HIDDEN NOMINAL/RECOVERY:
     *  Never show (smoke and mirrors)
     */
    public enum VisibilityMode {

        Full, Background, None
    };
    // Shared
    public static final Color BACKGROUND_COLOR = new Color(255, 255, 255);
//    public static final Font TEXT_FONT = new Font("Dialog", Font.BOLD, 28);
    public static final Font TEXT_FONT = new Font("Dialog", Font.BOLD, 16);
    public static final int LABEL_OFFSET = 0;
//    public static final int LABEL_OFFSET = 50; // interrupt plan
    // Vertex
    public static final Color VERTEX_COLOR = new Color(0, 0, 0);
    public static final Color SEL_VERTEX_COLOR = new Color(160, 0, 160);
    public static final boolean DRAW_LABELS = true;
    public static final String LABEL_TEXT_COLOR = "rgb(88,108,255)";
    public static final String SEL_LABEL_TEXT_COLOR = "rgb(160,0,160)";
    public static final boolean DRAW_MARKUPS = true;
    public static final String MARKUP_TEXT_COLOR = "rgb(200,40,200)";
    public static final Color BKGND_VERTEX_COLOR = new Color(0, 0, 0);
    // Place
    public static final Color START_PLACE_COLOR = new Color(82, 170, 82);
    public static final Color END_PLACE_COLOR = new Color(184, 79, 58);
    public static final Color PLACE_COLOR = new Color(127, 189, 246);
    public static final boolean DRAW_SUB_MISSIONS = true;
    public static final String SUB_MISSION_TEXT_COLOR_TEMPLATE = "rgb(99,99,99)";
    public static final String SUB_MISSION_TEXT_COLOR_INCOMPLETE = "rgb(188,6,6)";
    public static final String SUB_MISSION_TEXT_COLOR_COMPLETE = "rgb(0,145,0)";
    public static final boolean DRAW_TOKENS = true;
    public static final String TOKEN_TEXT_COLOR = "rgb(160,0,160)";
    // Transition
    public static final Color TRANSITION_COLOR = new Color(210, 210, 210);
    // Events
    public static final boolean DRAW_EVENTS = true;
    public static final String INPUT_EVENT_TEXT_COLOR_INACTIVE = "rgb(50,50,50)";
    public static final String INPUT_EVENT_TEXT_COLOR_INCOMPLETE = "rgb(188,6,6)";
    public static final String INPUT_EVENT_TEXT_COLOR_COMPLETE = "rgb(0,145,0)";
    public static final String OUTPUT_EVENT_TEXT_COLOR = "rgb(0,0,0)";
    // Edge
    public static final Color EDGE_COLOR = new Color(0, 0, 0);
    public static final Color SEL_EDGE_COLOR = new Color(160, 0, 160);
    public static final boolean DRAW_TOKEN_REQS = true;
    public static final String TOKEN_REQ_TEXT_COLOR = "rgb(188,6,6)";
    public static final int MAX_STRING_LENGTH = 100;
    public static final Color INVIS_EDGE_COLOR = null;
    public static final Stroke NOMINAL_STROKE = new BasicStroke(1);
    public static final Stroke NOMINAL_STROKE_SEL = new BasicStroke(10);
    public static final Stroke RECOVERY_STROKE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
    public static final Stroke RECOVERY_STROKE_SEL = new BasicStroke(10, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
}
