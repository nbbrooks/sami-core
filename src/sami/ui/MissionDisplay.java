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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
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
import sami.gui.GuiConfig.VisibilityMode;
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
    private final MissionMonitor missionMonitor;
    private final MissionPlanSpecification mSpec;
    private final PlanManager pm;
    ArrayList<Place> filledPlaces = new ArrayList<Place>();
    VisualizationViewer vv;
    AbstractLayout<Vertex, Edge> layout;
    Graph<Vertex, Edge> graph;
    private JPanel controlP;
    private GraphZoomScrollPane viewerP;
    // Need both preferred and max
    private final Dimension EXPANDED_DIM = new Dimension(400, 250);
    private final Dimension COLLAPSED_DIM = new Dimension(400, 30);
    private final Dimension CONTROL_BAR_DIM = new Dimension(400, 30);
    private final Dimension VIEWER_DIM = new Dimension(400, 190);
    private JButton visibilityB, abortB, followPlanB, snapViewB;
    private boolean collapsed = false, followPlan = false;
    private JLabel eventCounterL, nameL;
    private int missedEventCounter = 0;

    // Recovery visbility sub-graphs lookups
    // Vertices to hide when recovery transition sub-graph becomes inactive
    Hashtable<Transition, ArrayList<Vertex>> firstTransitionToVertices = new Hashtable<Transition, ArrayList<Vertex>>();
    // Edges to hide when recovery transition sub-graph becomes inactive
    Hashtable<Transition, ArrayList<Edge>> firstTransitionToEdges = new Hashtable<Transition, ArrayList<Edge>>();
    // Places in recovery transition sub-graph which are active - when empty, hide the sub-graph
    Hashtable<Transition, ArrayList<Place>> firstTransitionToActivePlaces = new Hashtable<Transition, ArrayList<Place>>();
    // Whether first transition's sub-graph should be visible
    Hashtable<Transition, Boolean> firstTransitionToVisibility = new Hashtable<Transition, Boolean>();
    // The first transitions for which the place is a member of its sub-graph
    Hashtable<Place, ArrayList<Transition>> placeToFirstTransitions = new Hashtable<Place, ArrayList<Transition>>();

    public MissionDisplay(MissionMonitor missionMonitor, MissionPlanSpecification mSpec, PlanManager pm) {
        this.missionMonitor = missionMonitor;
        this.mSpec = mSpec;
        this.pm = pm;

        // Initialize sub-graph visibility lookups
        initTables();
        // Create control button panel
        createControlPanel();
        // Create mission view panel
        createViewerPanel();
        loadGraph();

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
        boolean repaint = false;
        // Change drawing mode
        if (!filledPlaces.contains(p)) {
            filledPlaces.add(p);
            repaint = true;
        }
        // For each first recovery transition sub-graph this is a member of, add it to the respective tracking list
        if (placeToFirstTransitions.containsKey(p)) {
            ArrayList<Transition> firstTransitions = placeToFirstTransitions.get(p);
            for (Transition firstTransition : firstTransitions) {
                if (!firstTransitionToActivePlaces.get(firstTransition).contains(p)) {
                    firstTransitionToActivePlaces.get(firstTransition).add(p);
                }
            }
        }
        if (followPlan) {
            snapViewToActive();
            repaint = true;
        }
        if (repaint) {
            vv.repaint();
        }
    }

    @Override
    public void planLeftPlace(PlanManager planManager, Place p) {
        if (planManager != pm) {
            return;
        }
        boolean repaint = false;
        // For each first recovery transition sub-graph this is a member of, remove it from the respective tracking list
        if (placeToFirstTransitions.containsKey(p)) {
            ArrayList<Transition> firstTransitions = placeToFirstTransitions.get(p);
            for (Transition firstTransition : firstTransitions) {
                ArrayList<Place> activePlaces = firstTransitionToActivePlaces.get(firstTransition);
                if (activePlaces.remove(p)) {
                    // If the tracking list is empty, hide the sub-graph
                    if (activePlaces.isEmpty()) {
                        setSubGraphVisibility(firstTransition, false);
                        repaint = true;
                    }
                }
            }
        }
        if (filledPlaces.remove(p)) {
            repaint = true;
        }
        if (followPlan) {
            snapViewToActive();
            repaint = true;
        }
        if (repaint) {
            vv.repaint();
        }
    }

    @Override
    public void planRepaint(PlanManager planManager) {
        if (planManager != pm) {
            return;
        }
        vv.repaint();
    }

    @Override
    public void planFinished(PlanManager planManager) {
        if (planManager != pm) {
            return;
        }
        vv.repaint();
    }

    @Override
    public void planExecutedTransition(PlanManager planManager, Transition transition) {
        if (planManager != pm) {
            return;
        }
        boolean repaint = false;
        // If this is a first transition, and its sub-graph is not visible, make it visible
        //  @todo Do this in planEnteredPlace instead?
        if (isFirstTransition(transition)) {
            if (setSubGraphVisibility(transition, true)) {
                repaint = true;
            }
        }
        if (repaint) {
            vv.repaint();
        }
    }

    @Override
    public void planAborted(PlanManager planManager) {
    }

    private void initTables() {
        for (Vertex vertex : mSpec.getGraph().getVertices()) {
            if (vertex instanceof Transition && vertex.getFunctionMode() == Vertex.FunctionMode.Recovery) {
                // Check if any incoming places have nominal function mode
                Transition t = (Transition) vertex;
                for (Edge e : t.getInEdges()) {
                    if (e.getStart().getFunctionMode() == Vertex.FunctionMode.Nominal) {
                        // This is a "first transition"
                        ArrayList<Place> places = new ArrayList<Place>();
                        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
                        ArrayList<Edge> edges = new ArrayList<Edge>();
                        firstTransitionToActivePlaces.put(t, places);
                        firstTransitionToVertices.put(t, vertices);
                        firstTransitionToEdges.put(t, edges);
                        firstTransitionToVisibility.put(t, false);
                        addHiddenSubgraph(t, e, vertices, edges);
                        break;
                    }
                }
            }
        }
    }

    private void addHiddenSubgraph(Transition transition, Edge inEdge, ArrayList<Vertex> vertices, ArrayList<Edge> edges) {
        vertices.add(transition);
        edges.add(inEdge);
        for (Edge e : transition.getOutEdges()) {
            if (e.getFunctionMode() == Vertex.FunctionMode.Recovery && !edges.contains(e)) {
                edges.add(e);
            }
            Place p2 = (Place) e.getEnd();
            if (p2.getFunctionMode() == Vertex.FunctionMode.Recovery && !vertices.contains(p2)) {
                vertices.add(p2);
                for (Edge e2 : p2.getOutEdges()) {
                    if (e2.getEnd().getFunctionMode() == Vertex.FunctionMode.Recovery) {
                        addHiddenSubgraph((Transition) e2.getEnd(), e2, vertices, edges);
                    } else if (e2.getFunctionMode() == Vertex.FunctionMode.Recovery && !edges.contains(e2)) {
                        // Recovery transition to recovery place that has already been added - still add the new edge
                        edges.add(e2);
                    }
                }
            }
        }
    }

    private boolean isFirstTransition(Transition t) {
        return firstTransitionToActivePlaces.containsKey(t);
    }

    public void initGraphVisibility() {
        for (Vertex vertex : graph.getVertices()) {
            switch (vertex.getFunctionMode()) {
                case Nominal:
                    vertex.setVisibilityMode(GuiConfig.VisibilityMode.Full);
                    break;
                default:
                    vertex.setVisibilityMode(GuiConfig.VisibilityMode.None);
                    break;
            }
        }
        for (Edge edge : graph.getEdges()) {
            if (edge.getStart().getFunctionMode() == Vertex.FunctionMode.Nominal && edge.getEnd().getFunctionMode() == Vertex.FunctionMode.Nominal) {
                edge.setVisibilityMode(GuiConfig.VisibilityMode.Full);
            } else {
                edge.setVisibilityMode(GuiConfig.VisibilityMode.None);
            }
        }
    }

    /**
     *
     * @param transition First transition of the sub-graph
     * @param visible Whether the sub-graph should be visible on account of the
     * first transition
     * @return Whether a repaint is needed
     */
    private boolean setSubGraphVisibility(Transition transition, boolean visible) {
        if (firstTransitionToVisibility.get(transition) != visible) {
            firstTransitionToVisibility.put(transition, visible);
            VisibilityMode mode = visible ? VisibilityMode.Full : VisibilityMode.Background;
            for (Vertex v : firstTransitionToVertices.get(transition)) {
                v.setVisibilityMode(mode);
            }
            for (Edge e : firstTransitionToEdges.get(transition)) {
                e.setVisibilityMode(mode);
            }

            // If we made a sub-graph non-visible, check visibility for members of visible first transition sub-graphs
            //  (something in the sub-graph we just hid could be a member of a still-visible sub-graph of another first transition)
            for (Transition visibleT : firstTransitionToVisibility.keySet()) {
                if (firstTransitionToVisibility.get(visibleT)) {
                    for (Vertex v : firstTransitionToVertices.get(visibleT)) {
                        v.setVisibilityMode(VisibilityMode.Full);
                    }
                    for (Edge e : firstTransitionToEdges.get(visibleT)) {
                        e.setVisibilityMode(VisibilityMode.Full);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void createViewerPanel() {
        // Create visualization
        layout = new DAGLayout<Vertex, Edge>(new SparseMultigraph<Vertex, Edge>());
        vv = new VisualizationViewer<Vertex, Edge>(layout);

        // Visualization settings
        vv.setBackground(GuiConfig.BACKGROUND_COLOR);

        // EDGE
        vv.getRenderContext().setArrowDrawPaintTransformer(new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.EDGE_COLOR;
                    case None:
                    default:
                        return GuiConfig.INVIS_EDGE_COLOR;
                }
            }
        });

        vv.getRenderContext().setArrowFillPaintTransformer(new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.EDGE_COLOR;
                    case None:
                    default:
                        return GuiConfig.INVIS_EDGE_COLOR;
                }
            }
        });

        vv.getRenderContext().setEdgeDrawPaintTransformer(new Transformer<Edge, Paint>() {
            public Paint transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.EDGE_COLOR;
                    case None:
                    default:
                        return GuiConfig.INVIS_EDGE_COLOR;
                }
            }
        });

        vv.getRenderContext().setEdgeFontTransformer(new Transformer<Edge, Font>() {
            @Override
            public Font transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                    case Background:
                        return GuiConfig.TEXT_FONT;
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.getRenderContext().setLabelOffset(GuiConfig.LABEL_OFFSET);
        vv.getRenderContext().setEdgeLabelTransformer(new Transformer<Edge, String>() {
            @Override
            public String transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                        return edge.getShortTag();
                    case Background:
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<Edge, Stroke>() {
            @Override
            public Stroke transform(Edge edge) {
                switch (edge.getVisibilityMode()) {
                    case Full:
                        if (edge.getFunctionMode() == Vertex.FunctionMode.Recovery) {
                            return GuiConfig.RECOVERY_STROKE;
                        } else {
                            return GuiConfig.NOMINAL_STROKE;
                        }
                    case Background:
                    case None:
                    default:
                        return null;
                }
            }
        });

        // VERTEX
        vv.getRenderContext()
                .setVertexDrawPaintTransformer(new Transformer<Vertex, Paint>() {
                    @Override
                    public Paint transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                                return GuiConfig.VERTEX_COLOR;
                            case Background:
                                return GuiConfig.BKGND_VERTEX_COLOR;
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        vv.getRenderContext()
                .setVertexFillPaintTransformer(new Transformer<Vertex, Paint>() {
                    @Override
                    public Paint transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                                if (vertex instanceof Place) {
                                    Place place = (Place) vertex;
                                    if (place.isStart()) {
                                        return GuiConfig.START_PLACE_COLOR;
                                    } else if (place.isEnd()) {
                                        return GuiConfig.END_PLACE_COLOR;
                                    } else {
                                        return GuiConfig.PLACE_COLOR;
                                    }
                                } else if (vertex instanceof Transition) {
                                    return GuiConfig.TRANSITION_COLOR;
                                }
                                return null;
                            case Background:
                                return GuiConfig.BKGND_VERTEX_COLOR;
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        vv.getRenderContext()
                .setVertexFontTransformer(new Transformer<Vertex, Font>() {
                    @Override
                    public Font transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                            case Background:
                                return GuiConfig.TEXT_FONT;
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        vv.getRenderContext().setVertexLabelTransformer(new Transformer<Vertex, String>() {
            @Override
            public String transform(Vertex vertex) {
                switch (vertex.getVisibilityMode()) {
                    case Full:
                        return vertex.getShortTag();
                    case Background:
                    case None:
                    default:
                        return null;
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

        vv.getRenderContext().setVertexStrokeTransformer(new Transformer<Vertex, Stroke>() {
            @Override
            public Stroke transform(Vertex vertex) {
                switch (vertex.getVisibilityMode()) {
                    case Full:
                        if (vertex.getFunctionMode() == Vertex.FunctionMode.Recovery) {
                            return GuiConfig.RECOVERY_STROKE;
                        } else {
                            return GuiConfig.NOMINAL_STROKE;
                        }
                    case Background:
                        if (vertex.getFunctionMode() == Vertex.FunctionMode.Recovery) {
                            return GuiConfig.RECOVERY_STROKE;
                        } else {
                            return GuiConfig.NOMINAL_STROKE;
                        }
                    case None:
                    default:
                        return null;
                }
            }
        });

        vv.setVertexToolTipTransformer(
                new Transformer<Vertex, String>() {
                    @Override
                    public String transform(Vertex vertex) {
                        switch (vertex.getVisibilityMode()) {
                            case Full:
                                return vertex.getTag();
                            case Background:
                            case None:
                            default:
                                return null;
                        }
                    }
                });

        // Add mouse listener to visualization for panning and zooming the Petri Net
        MyMouseListener mml = new MyMouseListener();
        vv.addMouseListener(mml);
        vv.addMouseMotionListener(mml);
        vv.addMouseWheelListener(mml);

        // Encapsulate vizualization in scroll pane for buton-based panning and zooming
        viewerP = new GraphZoomScrollPane(vv);
        viewerP.setPreferredSize(VIEWER_DIM);
        viewerP.setMaximumSize(VIEWER_DIM);
        viewerP.revalidate();
    }
    
    private void loadGraph() {
        // Apply vertice locations
        mSpec.updateLayout(layout);
        graph = mSpec.getGraph();
        layout.setGraph(graph);

        initGraphVisibility();
        snapViewToVisible();
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
        // Visibility button
        visibilityB = new JButton("Collapsed: OFF");
        visibilityB.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        toggleMissionViewer();
                        visibilityB.setText("Collapsed: " + (collapsed ? "ON" : "OFF"));
                    }
                });
        // Plan "follow" toggle button
        //  Follow will keep the Mission Display centered around the active places of the plan instance
        followPlanB = new JButton("Follow: OFF");
        followPlanB.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        toggleFollowPlan();
                        followPlanB.setText("Follow: " + (followPlan ? "ON" : "OFF"));
                    }
                });
        // Snap view button
        //  Sets Mission Display view to contain the visible vertices of the plan instance
        snapViewB = new JButton("Snap View");
        snapViewB.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        snapViewToVisible();
                        vv.repaint();
                    }
                });

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
        // Lay out right-aligned components
        JPanel rightAlignP = new JPanel(new GridBagLayout());
        c.gridx = 0;
        c.anchor = GridBagConstraints.EAST;
        rightAlignP.add(eventCounterL, c);
        c.gridx++;
        rightAlignP.add(followPlanB, c);
        c.gridx++;
        rightAlignP.add(snapViewB, c);
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
        pm.eventGenerated(new AbortMissionReceived(pm.missionId));
    }

    public void toggleMissionViewer() {
        collapsed = !collapsed;
        if (collapsed) {
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

    private void toggleFollowPlan() {
        followPlan = !followPlan;
        if (followPlan) {
            snapViewToActive();
            vv.repaint();
        }
    }

    private void snapViewToActive() {
        double[] corners = getActiveMissionDims();
        setCorners(corners);
    }

    private void snapViewToVisible() {
        double[] corners = getVisibleMissionDims();
        setCorners(corners);
    }

    /**
     * Adjust visualization transform so that a specified rectangle in vertex
     * space is visible
     *
     * @param corners A double[4] set to {min x point, min y point, max x point,
     * max y point}
     */
    private void setCorners(double[] corners) {
        if (Double.isNaN(corners[0]) || Double.isNaN(corners[1]) || Double.isNaN(corners[2]) || Double.isNaN(corners[3])) {
            return;
        }

        // Pad edges to account for shape dimensions, event/markup text, etc
        // @todo could have this customized to corner vertices' properties
        corners[0] -= 15;
        corners[1] -= 15;
        corners[2] += 30;
        corners[3] += 30;

        // Calculate scale and translation transform
        double[] transform = new double[6];
        transform[0] = -VIEWER_DIM.width / (corners[0] - corners[2]);
        transform[3] = -VIEWER_DIM.height / (corners[1] - corners[3]);
        // Use 1:1 zoom ratio
        double adjScale = Math.min(transform[0], transform[3]);
        transform[0] = adjScale;
        transform[3] = adjScale;
        transform[4] = -corners[0] * transform[0];
        transform[5] = -corners[1] * transform[3];
        AffineTransform at = new AffineTransform(transform);
        // Mimic CrossoverScalingControl zooming
        //  View layer's scale must be <= 1
        //  Layout layers's scale must be >= 1
        MultiLayerTransformer mlt = vv.getRenderContext().getMultiLayerTransformer();
        if (adjScale < 1) {
            mlt.getTransformer(Layer.VIEW).getTransform().setTransform(at);
            mlt.getTransformer(Layer.LAYOUT).getTransform().setToScale(1.0, 1.0);
        } else {
            mlt.getTransformer(Layer.LAYOUT).getTransform().setTransform(at);
            mlt.getTransformer(Layer.VIEW).getTransform().setToScale(1.0, 1.0);
        }
    }

    /**
     * Return area capturing all places with tokens
     *
     * @return A double[4] set to {min x point, min y point, max x point, max y
     * point}
     */
    private double[] getActiveMissionDims() {
        return getCorners(0);
    }

    /**
     * Return area capturing all places with visibility mode full
     *
     * @return A double[4] set to {min x point, min y point, max x point, max y
     * point}
     */
    private double[] getVisibleMissionDims() {
        return getCorners(1);
    }

    /**
     * Return area capturing all vertices, ignoring visibility mode
     *
     * @return A double[4] set to {min x point, min y point, max x point, max y
     * point}
     */
    private double[] getFullMissionDims() {
        return getCorners(2);
    }

    private double[] getCorners(int mode) {
        // Mode
        //  0: Active
        //  1: Visible
        //  2: All

        double[] corners = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        Map<Vertex, Point2D> locations = mSpec.getLocations();
        for (Vertex vertex : locations.keySet()) {
            boolean add = false;
            switch (mode) {
                case 0:
                default:
                    add = vertex instanceof Place && ((Place) vertex).getIsActive();
                    break;
                case 1:
                    add = vertex.getVisibilityMode() == VisibilityMode.Full;
                    break;
                case 2:
                    add = true;
                    break;
            }
            if (add) {
                // Expand area to capture this point
                if (locations.get(vertex) != null) {
                    Point2D point = locations.get(vertex);
                    if (Double.isNaN(corners[0]) || point.getX() < corners[0]) {
                        corners[0] = point.getX();
                    }
                    if (Double.isNaN(corners[2]) || point.getX() > corners[2]) {
                        corners[2] = point.getX();
                    }
                    if (Double.isNaN(corners[1]) || point.getY() < corners[1]) {
                        corners[1] = point.getY();
                    }
                    if (Double.isNaN(corners[3]) || point.getY() > corners[3]) {
                        corners[3] = point.getY();
                    }
                } else {
                    LOGGER.severe("Vertex [" + vertex + "] has no graph location");
                }
            }
        }
        return corners;
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

            if ((me.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0
                    || (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK)) != 0
                    || (me.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) != 0) {
                // Mouse 2 OR Mouse1 + Shift OR Mouse1 + Mouse3
                amTranslating = true;
                prevMousePoint = (Point2D) framePoint.clone();
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
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
                MutableTransformer layout = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW);
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
