package sami.ui;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.MultiLayerTransformer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.apache.commons.collections15.Transformer;
import sami.engine.Engine;
import sami.engine.PlanManager;
import sami.engine.PlanManagerListenerInt;
import sami.event.AbortMissionReceived;
import sami.gui.GuiConfig;
import sami.mission.Edge;
import sami.mission.MissionPlanSpecification;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;

/**
 *
 * @author pscerri
 */
public class MissionDisplay extends JPanel implements PlanManagerListenerInt {

    private static final Logger LOGGER = Logger.getLogger(MissionDisplay.class.getName());
    private MissionMonitor missionMonitor;
    private MissionPlanSpecification mSpec;
    private PlanManager pm;
    ArrayList<Place> filledPlaces = new ArrayList<Place>();
    VisualizationViewer vv;
    AbstractLayout<Vertex, Edge> layout;
    Graph<Vertex, Edge> graph;
    private JPanel controlP;
    private GraphZoomScrollPane viewerP;
    boolean minimized = false;
    // Need both preferred and max
    private final Dimension EXPANDED_DIM = new Dimension(400, 360);
    private final Dimension COLLAPSED_DIM = new Dimension(400, 30);
    private final Dimension CONTROL_BAR_DIM = new Dimension(400, 30);
    private final Dimension VIEWER_DIM = new Dimension(400, 300);
    private JButton visibilityB, abortB, followB, abortHomeB;
    private JLabel eventCounterL, nameL;
    private int missedEventCounter = 0;

    public MissionDisplay(MissionMonitor missionMonitor, MissionPlanSpecification mSpec, PlanManager pm) {
        this.missionMonitor = missionMonitor;
        this.mSpec = mSpec;
        this.pm = pm;

        createControlPanel();
        createViewerPanel();
        setLayout(new BorderLayout());
        add(controlP, BorderLayout.NORTH);
        add(viewerP, BorderLayout.CENTER);
        setPreferredSize(EXPANDED_DIM);
        setMaximumSize(EXPANDED_DIM);
        revalidate();

        Engine.getInstance().addListener(this);

        (new Thread() {
            public void run() {
            }
        }).start();
    }

    @Override
    public void planCreated(PlanManager planManager, MissionPlanSpecification spec) {
    }

    @Override
    public void planStarted(PlanManager planManager) {
    }

    @Override
    public void planEnteredPlace(PlanManager planManager, Place p) {
        if (planManager != pm) {
            return;
        }
        if (!filledPlaces.contains(p)) {
            filledPlaces.add(p);
            vv.repaint();
        }
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place p) {
        if (planManager != pm) {
            return;
        }
        if (filledPlaces.remove(p)) {
            vv.repaint();
        }
    }

    @Override
    public void planFinished(PlanManager planManager) {
        if (planManager != pm) {
            return;
        }
        vv.repaint();
    }

    @Override
    public void planAborted(PlanManager planManager) {
    }

    private void createViewerPanel() {
        // Create graph viewer
        Graph<Vertex, Edge> graph = new SparseMultigraph<Vertex, Edge>();
        AbstractLayout<Vertex, Edge> layout = new DAGLayout<Vertex, Edge>(graph); // StaticLayout<Place, Transition>(graph, new Dimension(600, 600));
        vv = new VisualizationViewer<Vertex, Edge>(layout);
        vv.setBackground(GuiConfig.BACKGROUND_COLOR);
        vv.getRenderContext().setVertexLabelTransformer(new Transformer<Vertex, String>() {
            @Override
            public String transform(Vertex vertex) {
                return vertex.getShortTag();
            }
        });
        vv.getRenderContext().setVertexFontTransformer(new Transformer<Vertex, Font>() {
            @Override
            public Font transform(Vertex i) {
                return new java.awt.Font("Dialog", Font.BOLD, 14);
            }
        });
        vv.setVertexToolTipTransformer(new Transformer<Vertex, String>() {
            @Override
            public String transform(Vertex vertex) {
                return vertex.getTag();
            }
        });
        vv.getRenderContext().setEdgeFontTransformer(new Transformer<Edge, Font>() {
            @Override
            public Font transform(Edge i) {
                return new java.awt.Font("Dialog", Font.BOLD, 14);
            }
        });
        vv.getRenderContext().setEdgeLabelTransformer(new Transformer<Edge, String>() {
            @Override
            public String transform(Edge edge) {
                return edge.getShortTag();
            }
        });
        vv.getRenderContext().setVertexStrokeTransformer(new Transformer<Vertex, Stroke>() {
            @Override
            public Stroke transform(Vertex vertex) {
                if (filledPlaces.contains(vertex)) {
                    return new BasicStroke(10);
                } else if (vertex instanceof Place && ((Place) vertex).getSubMissions() != null && !((Place) vertex).getSubMissions().isEmpty()) {
                    return new BasicStroke(5);
                } else {
                    return new BasicStroke(1);
                }
            }
        });
        vv.getRenderContext().setVertexDrawPaintTransformer(new Transformer<Vertex, Paint>() {
            @Override
            public Paint transform(Vertex vertex) {
                if (filledPlaces.contains(vertex)) {
                    return GuiConfig.SEL_VERTEX_COLOR;
                } else {
                    return Color.BLACK;
                }
            }
        });
        vv.getRenderContext().setVertexFillPaintTransformer(new Transformer<Vertex, Paint>() {
            @Override
            public Paint transform(Vertex vertex) {
                if (vertex instanceof Place) {
                    Place place = (Place) vertex;
                    if (place.isStart()) {
                        return GuiConfig.START_PLACE_COLOR;
                    } else if (place.isEnd()) {
                        return GuiConfig.END_PLACE_COLOR;
                    } else {
                        return GuiConfig.PLACE_COLOR;
                    }
                } else {
                    return GuiConfig.TRANSITION_COLOR;
                }
            }
        });
        vv.getRenderContext().setVertexShapeTransformer(new Transformer<Vertex, Shape>() {
            @Override
            public Shape transform(Vertex vertex) {
                if (vertex instanceof Transition) {
                    return ((Transition) vertex).getShape();
                } else if (vertex instanceof Place) {
                    return ((Place) vertex).getShape();
                } else {
                    return null;
                }
            }
        });

        MissionDisplay.MyMouseListener mml = new MissionDisplay.MyMouseListener();
        vv.addMouseListener(mml);
        vv.addMouseMotionListener(mml);
        vv.addMouseWheelListener(mml);

        if (mSpec.getGraph() != null) {
            SparseMultigraph uGraph = (SparseMultigraph) mSpec.getGraph();
            for (Object o : uGraph.getVertices()) {
                graph.addVertex((Vertex) o);
            }
            for (Object o : uGraph.getEdges()) {
                Edge e = (Edge) o;
                graph.addEdge(e, e.getStart(), e.getEnd());
            }

            MultiLayerTransformer mlt = vv.getRenderContext().getMultiLayerTransformer();
            mlt.getTransformer(Layer.LAYOUT).getTransform().setTransform(mSpec.getLayoutTransform());
            mlt.getTransformer(Layer.VIEW).getTransform().setTransform(mSpec.getView());

            layout.setGraph(graph);
            mSpec.updateThisLayout(layout);
            vv.repaint();
        }

        viewerP = new GraphZoomScrollPane(vv);
        viewerP.setPreferredSize(VIEWER_DIM);
        viewerP.setMaximumSize(VIEWER_DIM);
        viewerP.revalidate();
    }

    private void createControlPanel() {
        // Define control bar components
        // Name of the plan
        nameL = new JLabel(pm.getPlanName());
        // Count number of missed "events" (transitions?) that have occurred in the plan since the operator last interacted with it 
        eventCounterL = new JLabel("0");
        // Abort button
        abortB = new JButton("Abort");
        abortB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int ret = JOptionPane.showConfirmDialog(null, "Really abort?");
                if (ret == JOptionPane.OK_OPTION) {
                    abortMission();
                }
            }
        });
        abortHomeB = new JButton("Abort and Return Home");
        abortHomeB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int ret = JOptionPane.showConfirmDialog(null, "Really abort?");
                if (ret == JOptionPane.OK_OPTION) {
                    abortMission();
                   
                    for(int i = 0; i < missionMonitor.missionListModel.size(); i++){
                        MissionPlanSpecification p = (MissionPlanSpecification) missionMonitor.missionListModel.get(i);
                        
                            if(p.getName().equals("Return To Base")){
                                pm = Engine.getInstance().spawnRootMission(p);
                                
                                return;
                            
                        }
                    }
                }
            }
        });
        // Visibility button
        visibilityB = new JButton("Collapsed: OFF");
        visibilityB.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        toggleMissionViewer();
                        if (minimized) {
                            visibilityB.setText("Collapsed: ON");
                        } else {
                            visibilityB.setText("Collapsed: OFF");
                        }
                    }
                });
        // Plan "follow" button
        followB = new JButton("Follow: OFF");

        // Lay out left-aligned components
        JPanel leftAlignP = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.WEST;
        leftAlignP.add(nameL, c);
        c.gridx++;
        leftAlignP.add(abortB, c);
        c.gridx++;
        leftAlignP.add(abortHomeB, c);
        // Lay out right-aligned components
        JPanel rightAlignP = new JPanel(new GridBagLayout());
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        rightAlignP.add(eventCounterL, c);
        c.gridx++;
        rightAlignP.add(followB, c);
        c.gridx++;
        rightAlignP.add(visibilityB, c);
        // Combine left and right aligned panels into control bar
        controlP = new JPanel(new BorderLayout());
        controlP.setBorder(BorderFactory.createLineBorder(Color.black));
        controlP.add(leftAlignP, BorderLayout.WEST);
        controlP.add(rightAlignP, BorderLayout.EAST);
        controlP.setPreferredSize(CONTROL_BAR_DIM);
        controlP.setMaximumSize(CONTROL_BAR_DIM);
        controlP.revalidate();
    }

    public void abortMission() {
//            Engine.getInstance().getUiServer().UIMessage(new AbortMissionMessage(pm.missionId));
//        Engine.getInstance().abortMission(pm.missionId);
        pm.eventGenerated(new AbortMissionReceived(pm.missionId));
    }

    public void toggleMissionViewer() {
        minimized = !minimized;
        if (minimized) {
            hideMissionViewer();
        } else {
            showMissionViewer();
        }
        this.revalidate();
        missionMonitor.refreshMissionDisplay();
    }

    public void showMissionViewer() {
        viewerP.setVisible(true);
        this.setPreferredSize(EXPANDED_DIM);
        this.setMaximumSize(EXPANDED_DIM);
    }

    public void hideMissionViewer() {
        viewerP.setVisible(false);
        this.setPreferredSize(COLLAPSED_DIM);
        this.setMaximumSize(COLLAPSED_DIM);
    }

    private class MyMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener {

        final CrossoverScalingControl scaler = new CrossoverScalingControl();
        boolean amTranslating = false;
        Point2D prevMousePoint = null;
        double translationX = 0, translationY = 0, zoom = 1;

        @Override
        public void mouseClicked(MouseEvent me) {
        }

        @Override
        public void mousePressed(MouseEvent me) {
//            System.out.println("Pressed " + e.getButton());
            final Point2D framePoint = me.getPoint();

            if (me.getButton() == MouseEvent.BUTTON1
                    || me.getButton() == MouseEvent.BUTTON2
                    || (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) == (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) {
                amTranslating = true;
                prevMousePoint = (Point2D) framePoint.clone();
            } else if (me.getButton() == MouseEvent.BUTTON3) {
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
//             System.out.println("Released " + me.getButton());

            amTranslating = false;
            prevMousePoint = null;
        }

        @Override
        public void mouseEntered(MouseEvent me) {
        }

        @Override
        public void mouseExited(MouseEvent me) {
        }

        @Override
        public void mouseDragged(MouseEvent me) {
//            System.out.println("Dragged " + me.getButton());
            final Point2D framePoint = me.getPoint();

            if (amTranslating && prevMousePoint != null) {
                // Translate frame
                // The Render transform doesn't update very quickly, so do it ourselves so translation looks smooth
                MutableTransformer layout = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
                double scale = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).getScale();
                double deltaX = (framePoint.getX() - prevMousePoint.getX()) * 1 / scale;
                double deltaY = (framePoint.getY() - prevMousePoint.getY()) * 1 / scale;
                layout.translate(deltaX, deltaY);
                prevMousePoint = framePoint;
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent me) {
            if (me.getWheelRotation() < 0) {
                // Zoom in
                scaler.scale(vv, 1.1f, me.getPoint());
                zoom *= 1.1;
            } else if (me.getWheelRotation() > 0) {
                // Zoom out
                scaler.scale(vv, 1 / 1.1f, me.getPoint());
                zoom /= 1.1;
            }
        }
    }
}
