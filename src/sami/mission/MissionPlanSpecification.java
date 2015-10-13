package sami.mission;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.engine.Mediator;
import sami.event.Event;
import sami.event.InputEvent;
import sami.event.OutputEvent;
import sami.event.ReflectedEventSpecification;
import sami.markup.ReflectedMarkupSpecification;

/**
 *
 * @author pscerri
 */
public class MissionPlanSpecification implements java.io.Serializable {

    private static final Logger LOGGER = Logger.getLogger(MissionPlanSpecification.class.getName());
    static final long serialVersionUID = 2L;
    private AffineTransform layoutTransform = new AffineTransform();
    private AffineTransform viewTransform = new AffineTransform();
    // List of tasks created in the spec by the developer
    private ArrayList<TaskSpecification> taskSpecList = new ArrayList<TaskSpecification>();
    // We want graph to be a DirectedSparseGraph so we render the arrows on the edges
    //  However, DirectedSparseGraph can not be serialized because one of its fields does not have a no-arg constructor
    //  Our SparseMultigraph can be serialized though
    //  We write all changes to both graphs, but create DirectedSparseGraph from the SparseMultigraph when we read in the mSpec
    private Graph<Vertex, Edge> graph = new SparseMultigraph<Vertex, Edge>();
    transient private DirectedSparseGraph<Vertex, Edge> transientGraph = null;
    private Map<Vertex, ArrayList<ReflectedEventSpecification>> vertexToEventSpecListMap = new HashMap<Vertex, ArrayList<ReflectedEventSpecification>>();
    // [x, y] coordinates of the places and transitions
    //  We don't serialize the VisualizationViewer's AbstractLayout
    //  Instead we store the coordinate lookup in locations
    //  We write all changes to locations and the layout, but write the values from locations into layout each time we load the mSpec into TaskModelEditor
    private Map<Vertex, Point2D> locations = null;
    private transient AbstractLayout<Vertex, Edge> layout = null;
    private String name = "Anonymous";
    transient private boolean isInstantiated = false;
    public static final String RETURN_SUFFIX = ".return";

    public MissionPlanSpecification(String name) {
        this.name = name;
        transientGraph = new DirectedSparseGraph<Vertex, Edge>();
        locations = new Hashtable<Vertex, Point2D>();
        layout = new StaticLayout<Vertex, Edge>(transientGraph, new Dimension(600, 600));
    }

    public MissionPlanSpecification getSubmissionInstance(MissionPlanSpecification parentSpec, String namePrefix, String variablePrefix, HashMap<String, Object> globalVariables) {
        MissionPlanSpecification copy = deepClone();

        // Apply prefixes
        copy.setName(namePrefix + "." + name);
        copy.addVariablePrefix(variablePrefix, globalVariables);

        return copy;
    }

    public ArrayList<String> getAllVariables() {
        ArrayList<String> variables = new ArrayList<String>();

        for (Vertex v : graph.getVertices()) {
            if (v instanceof Transition) {
                if (vertexToEventSpecListMap.containsKey(v)) {
                    for (ReflectedEventSpecification eventSpec : vertexToEventSpecListMap.get(v)) {
                        for (String readVariable : eventSpec.getReadVariables().values()) {
                            if (!variables.contains(readVariable)) {
                                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Adding \"" + readVariable + "\" to variable list");
                                variables.add(readVariable);
                            }
                        }
                        for (String writeVariable : eventSpec.getWriteVariables().values()) {
                            if (!variables.contains(writeVariable)) {
                                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Adding \"" + writeVariable + "\" to variable list");
                                variables.add(writeVariable);
                            }
                        }
                    }
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINE, "No events to check for variables.");
                }
            }
        }
        return variables;
    }

    public void addVariablePrefix(String prefix, HashMap<String, Object> globalVariableNames) {
        // Add prefix to all variables in the plan, recursing into sub-missions
        for (Vertex key : vertexToEventSpecListMap.keySet()) {
            ArrayList<ReflectedEventSpecification> value = vertexToEventSpecListMap.get(key);
            for (ReflectedEventSpecification eventSpec : value) {
                eventSpec.addVariablePrefix(prefix, globalVariableNames);
            }
            if (key instanceof Place && ((Place) key).getSubMissionTemplates() != null) {
                for (MissionPlanSpecification subMSpec : ((Place) key).getSubMissionTemplates()) {
                    subMSpec.addVariablePrefix(prefix, globalVariableNames);
                }
            }
        }
    }

    /**
     *
     * @return The transient DirectedSparseGraph copy of the existing
     * serializable graph
     */
    public Graph<Vertex, Edge> getTransientGraph() {
        if (graph == null) {
            return null;
        }
        if (transientGraph == null && graph != null) {
            // Copy the SparseMultigraph into a DirectedSparseGraph
            //  DirectedSparseGraph can not be serialized - one of its fields does not have a no-arg constructor
            transientGraph = new DirectedSparseGraph<Vertex, Edge>();
            for (Vertex o : graph.getVertices()) {
                transientGraph.addVertex(o);
            }
            for (Edge o : graph.getEdges()) {
                transientGraph.addEdge(o, o.getStart(), o.getEnd());
            }
        }
        return transientGraph;
    }

    /**
     *
     * @return The serializable SparseMultigraph
     */
    public Graph<Vertex, Edge> getSerializableGraph() {
        return graph;
    }

    public ArrayList<TaskSpecification> getTaskSpecList() {
        return taskSpecList;
    }

    /**
     * Returns RES with field(s) that do not have a definition
     *
     * @return
     */
    public ArrayList<ReflectedEventSpecification> getEventSpecsRequiringParams() {
        ArrayList<ReflectedEventSpecification> ret = new ArrayList<ReflectedEventSpecification>();
        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
            ArrayList<ReflectedEventSpecification> events = vertexToEventSpecListMap.get(vertex);
            for (ReflectedEventSpecification reflectedEventSpecification : events) {
                if (reflectedEventSpecification.hasMissingParams(false)) {
                    ret.add(reflectedEventSpecification);
                }
            }
        }
        return ret;
    }

    /**
     * Returns RES with field(s) that do not have a definition or are editable
     *
     * @return
     */
    public ArrayList<ReflectedEventSpecification> getEventSpecsRequestingParams() {
        ArrayList<ReflectedEventSpecification> ret = new ArrayList<ReflectedEventSpecification>();
        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
            ArrayList<ReflectedEventSpecification> events = vertexToEventSpecListMap.get(vertex);
            for (ReflectedEventSpecification reflectedEventSpecification : events) {
                if (reflectedEventSpecification.hasEditableParams(false)) {
                    ret.add(reflectedEventSpecification);
                }
            }
        }
        return ret;
    }

    public ArrayList<MissionPlanSpecification> getSharedMSpecs() {
        ArrayList<MissionPlanSpecification> sharedMSpecs = new ArrayList<MissionPlanSpecification>();
        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
            if (vertex instanceof Place) {
                Place place = (Place) vertex;
                for (MissionPlanSpecification mSpec : place.getSubMissionToIsSharedInstance().keySet()) {
                    if (place.getSubMissionToIsSharedInstance().get(mSpec)) {
                        // Shared mSpec, add to list to be spawned
                        sharedMSpecs.add(mSpec);
                    }
                }
            }
        }
        return sharedMSpecs;
    }

    /**
     * Looks for RES on places with fields that have both an object and write
     * variable definition
     *
     * @return
     */
    public Hashtable<String, Object> getDefinedOutputEventVariables() {
        Hashtable<String, Object> variableToValue = new Hashtable<String, Object>();
        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
            if (vertex instanceof Place) {
                if (vertexToEventSpecListMap.get(vertex) != null) {
                    ArrayList<ReflectedEventSpecification> events = vertexToEventSpecListMap.get(vertex);
                    for (ReflectedEventSpecification reflectedEventSpecification : events) {
                        HashMap<String, String> fieldNameToWriteVariable = reflectedEventSpecification.getWriteVariables();
                        HashMap<String, Object> fieldNameToValue = reflectedEventSpecification.getFieldValues();
                        for (String fieldName : fieldNameToWriteVariable.keySet()) {
                            if (fieldNameToValue.containsKey(fieldName)) {
                                variableToValue.put(fieldNameToWriteVariable.get(fieldName), fieldNameToValue.get(fieldName));
                            }
                        }
                    }
                }
            }
        }
        return variableToValue;
    }

    public Place getUninstantiatedStart() {
        for (Vertex v : graph.getVertices()) {
            if (v instanceof Place && ((Place) v).isStart()) {
                return (Place) v;
            }
        }
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "MissionPlanSpecification has no start state!");
        System.exit(0);
        return null;
    }

    public boolean isInstantiated() {
        return isInstantiated;
    }

    public void instantiate(UUID missionId) {
        LOGGER.info("Instantiating mission: " + name + " with id: " + missionId);
        // @todo MissionPlanSpecification getInstantiatedStart does not allow parameterization or reuse
        for (Vertex vertex : vertexToEventSpecListMap.keySet()) {
            ArrayList<ReflectedEventSpecification> eventSpecs = vertexToEventSpecListMap.get(vertex);
            for (ReflectedEventSpecification eventSpec : eventSpecs) {
                Event e = eventSpec.instantiate();
                e.setMissionId(missionId);
                if (vertex instanceof Transition && e instanceof InputEvent) {
                    ((Transition) vertex).addInputEvent((InputEvent) e);
                } else if (vertex instanceof Place && e instanceof OutputEvent) {
                    ((Place) vertex).addOutputEvent((OutputEvent) e);
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Have a mismatch of classes in vertexToEventSpecListMap!");
                    System.exit(0);
                }
            }
        }
        isInstantiated = true;
    }

    public void clearEventSpecList(Vertex vertex) {
        vertexToEventSpecListMap.put(vertex, new ArrayList<ReflectedEventSpecification>());
    }

    public void removeEventSpecList(Vertex vertex) {
        vertexToEventSpecListMap.remove(vertex);
    }

    public ArrayList<ReflectedEventSpecification> getEventSpecList(Vertex vertex) {
        return vertexToEventSpecListMap.get(vertex);
    }

    public void updateEventSpecList(Vertex vertex, ReflectedEventSpecification spec) {
        ArrayList<ReflectedEventSpecification> specList = vertexToEventSpecListMap.get(vertex);
        if (specList == null) {
            specList = new ArrayList<ReflectedEventSpecification>();
            vertexToEventSpecListMap.put(vertex, specList);
        }
        specList.add(spec);
    }

    public Map<Vertex, ArrayList<ReflectedEventSpecification>> getVertexToEventSpecListMap() {
        return vertexToEventSpecListMap;
    }

    public void setLayout(AffineTransform transform) {
        this.layoutTransform = transform;
    }

    public void setView(AffineTransform transform) {
        this.viewTransform = transform;
    }

    public AffineTransform getView() {
        return viewTransform;
    }

    public AffineTransform getLayoutTransform() {
        return layoutTransform;
    }

    /**
     * Updates a passed in Layout based on our Vertex locations object
     *
     * @param layout The Layout object to be updated
     */
    public void copyLocationsToLayout(AbstractLayout<Vertex, Edge> layout) {
        for (Vertex v : locations.keySet()) {
            if (locations.get(v) != null) {
                layout.setLocation(v, locations.get(v));
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public Map<Vertex, Point2D> getLocations() {
        return locations;
    }

    public AbstractLayout<Vertex, Edge> getLayout() {
        if (layout == null) {
            layout = new StaticLayout<Vertex, Edge>(getTransientGraph(), new Dimension(600, 600));
            if (locations != null) {
                for (Vertex v : locations.keySet()) {
                    if (locations.get(v) != null) {
                        layout.setLocation(v, locations.get(v));
                    }
                }
            }
        }
        return layout;
    }

    public void updateVertexLocation(Vertex v, Point2D point) {
        locations.put(v, point);
        layout.setLocation(v, point);
    }

    public void updateAllTags() {
        for (Vertex vertex : graph.getVertices()) {
            vertex.updateTag();
        }
        for (Edge edge : graph.getEdges()) {
            edge.updateTag();
        }
    }

    public void printGraph() {
        System.out.println("Printing " + name);
        int placeCount = 0, transitionCount = 0;
        for (Vertex v : graph.getVertices()) {
            if (v instanceof Place) {
                placeCount++;
            } else if (v instanceof Transition) {
                transitionCount++;
            }
        }
        System.out.println("\t Places: " + placeCount);
        for (Vertex v : graph.getVertices()) {
            if (v instanceof Place) {
                Place p = (Place) v;
                System.out.println("\t\t " + p.getTag());
                for (Transition t : p.getInTransitions()) {
                    System.out.println("\t\t\t in t: " + t.getTag());
                }
                for (Transition t : p.getOutTransitions()) {
                    System.out.println("\t\t\t out t: " + t.getTag());
                }
                for (Edge e : p.getInEdges()) {
                    System.out.println("\t\t\t in e: " + e.getTag());
                }
                for (Edge e : p.getOutEdges()) {
                    System.out.println("\t\t\t out e: " + e.getTag());
                }
            }
        }
        System.out.println("\t Transitions: " + transitionCount);
        for (Vertex v : graph.getVertices()) {
            if (v instanceof Transition) {
                Transition t = (Transition) v;
                System.out.println("\t\t " + ((Transition) v).getTag());
                for (Place p : t.getInPlaces()) {
                    System.out.println("\t\t\t in p: " + p.getTag());
                }
                for (Place p : t.getOutPlaces()) {
                    System.out.println("\t\t\t out p: " + p.getTag());
                }
                for (Edge e : t.getInEdges()) {
                    System.out.println("\t\t\t in e: " + e.getTag());
                }
                for (Edge e : t.getOutEdges()) {
                    System.out.println("\t\t\t out e: " + e.getTag());
                }
            }
        }
        System.out.println("\t Edges: " + graph.getEdges().size());
        for (Edge e : graph.getEdges()) {
            System.out.println("\t\t " + ((Edge) e).getTag());
            System.out.println("\t\t\t start: " + e.getStart());
            System.out.println("\t\t\t end: " + e.getEnd());
        }
    }

    public void addPlace(Place place, Point point) {
        graph.addVertex(place);
        transientGraph.addVertex(place);
        locations.put(place, point);
        layout.setLocation(place, point);
    }

    public void removePlace(Place place) {
        // First remove the place and its edges from the graphs
        for (Edge inEdge : place.getInEdges()) {
            graph.removeEdge(inEdge);
            transientGraph.removeEdge(inEdge);
        }
        for (Edge outEdge : place.getOutEdges()) {
            graph.removeEdge(outEdge);
            transientGraph.removeEdge(outEdge);
        }
        removeEventSpecList(place);
        graph.removeVertex(place);
        transientGraph.removeVertex(place);
        locations.remove(place);

        // Now we can remove the affected edges and references to this place
        place.removeReferences();
    }

    public void addTransition(Transition transition, Point point) {
        graph.addVertex(transition);
        transientGraph.addVertex(transition);
        locations.put(transition, point);
        layout.setLocation(transition, point);
    }

    public void removeTransition(Transition transition) {
        // First remove the transition and its edges from the graph
        for (InEdge inEdge : transition.getInEdges()) {
            graph.removeEdge(inEdge);
            transientGraph.removeEdge(inEdge);
        }
        for (OutEdge outEdge : transition.getOutEdges()) {
            graph.removeEdge(outEdge);
            transientGraph.removeEdge(outEdge);
        }
        removeEventSpecList(transition);
        graph.removeVertex(transition);
        transientGraph.removeVertex(transition);
        locations.remove(transition);

        // Now we can remove the affected edges and references to this transition
        transition.removeReferences();
    }

    public void addEdge(InEdge e, Place startPlace, Transition endTransition) {
        graph.addEdge(e, startPlace, endTransition);
        transientGraph.addEdge(e, startPlace, endTransition);
    }

    public void addEdge(OutEdge e, Transition startTransition, Place endPlace) {
        graph.addEdge(e, startTransition, endPlace);
        transientGraph.addEdge(e, startTransition, endPlace);
    }

    public void removeEdge(Edge edge) {
        // First remove the edge from the graph
        graph.removeEdge(edge);
        transientGraph.removeEdge(edge);

        // Now we can remove the references to this edge
        edge.removeReferences();
    }

    public void updateTags() {
        for (Vertex v : graph.getVertices()) {
            if (v instanceof Place) {
                ((Place) v).updateTag();
            } else if (v instanceof Transition) {
                ((Transition) v).updateTag();
            }
        }
        for (Edge e : graph.getEdges()) {
            if (e instanceof InEdge) {
                ((InEdge) e).updateTag();
            } else if (e instanceof OutEdge) {
                ((OutEdge) e).updateTag();
            }
        }
    }

    /**
     * Produces a copy of the passed in mission plan specification, probably for
     * creating a sub-mission
     */
    public MissionPlanSpecification deepClone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (MissionPlanSpecification) ois.readObject();
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Changes each vertex/edge to a mockup vertex/edge, copying over text for
     * each event/edge req
     *
     */
    public void convertToMockup() {
        Graph<Vertex, Edge> mockupGraph = new DirectedSparseGraph<Vertex, Edge>();
        Map<Vertex, Point2D> mockupLocations = new Hashtable<Vertex, Point2D>();

        Hashtable<Place, MockupPlace> placeLookup = new Hashtable<Place, MockupPlace>();
        Hashtable<Transition, MockupTransition> transitionLookup = new Hashtable<Transition, MockupTransition>();

        for (Vertex o : graph.getVertices()) {
            if (o instanceof MockupPlace || o instanceof MockupTransition) {
                mockupGraph.addVertex(o);
                mockupLocations.put(o, locations.get(o));
            } else if (o instanceof Place) {
                Place p = (Place) o;
                MockupPlace mp = new MockupPlace(p.getName(), Mediator.getInstance().getProject().getAndIncLastElementId());
                placeLookup.put(p, mp);
                mockupGraph.addVertex(mp);
                mockupLocations.put(mp, locations.get(p));

                Hashtable<String, ArrayList<String>> oeWithMarkup = new Hashtable<String, ArrayList<String>>();
                for (ReflectedEventSpecification res : p.eventSpecs) {
                    ArrayList<String> markupList = new ArrayList<String>();
                    for (ReflectedMarkupSpecification rms : res.getMarkupSpecs()) {
                        markupList.add(rms.getClassName().substring(rms.getClassName().lastIndexOf('.') + 1));
                    }
                    oeWithMarkup.put(res.getClassName().substring(res.getClassName().lastIndexOf('.') + 1), markupList);
                }
                mp.setMockupOutputEventMarkups(oeWithMarkup);
            } else if (o instanceof Transition) {
                Transition t = (Transition) o;
                MockupTransition mt = new MockupTransition(t.getName(), Mediator.getInstance().getProject().getAndIncLastElementId());
                transitionLookup.put(t, mt);
                mockupGraph.addVertex(mt);
                mockupLocations.put(mt, locations.get(t));

                Hashtable<String, ArrayList<String>> ieWithMarkup = new Hashtable<String, ArrayList<String>>();
                for (ReflectedEventSpecification res : t.eventSpecs) {
                    ArrayList<String> markupList = new ArrayList<String>();
                    for (ReflectedMarkupSpecification rms : res.getMarkupSpecs()) {
                        markupList.add(rms.getClassName().substring(rms.getClassName().lastIndexOf('.') + 1));
                    }
                    ieWithMarkup.put(res.getClassName().substring(res.getClassName().lastIndexOf('.') + 1), markupList);
                }
                mt.setMockupInputEventMarkups(ieWithMarkup);
            }
        }
        for (Edge o : graph.getEdges()) {
            if (o instanceof MockupInEdge || o instanceof MockupOutEdge) {
                mockupGraph.addEdge(o, o.getStart(), o.getEnd());
            } else if (o instanceof InEdge) {
                InEdge inEdge = (InEdge) o;
                MockupInEdge mie = new MockupInEdge(placeLookup.get(inEdge.getStart()), transitionLookup.get(inEdge.getEnd()), Mediator.getInstance().getProject().getAndIncLastElementId());
                mockupGraph.addEdge(mie, mie.getStart(), mie.getEnd());

                ArrayList<String> reqList = new ArrayList<String>();
                for (InTokenRequirement itr : inEdge.getTokenRequirements()) {
                    reqList.add(itr.toString());
                }
                mie.setMockupTokenRequirements(reqList);
            } else if (o instanceof OutEdge) {
                OutEdge outEdge = (OutEdge) o;
                MockupOutEdge moe = new MockupOutEdge(transitionLookup.get(outEdge.getStart()), placeLookup.get(outEdge.getEnd()), Mediator.getInstance().getProject().getAndIncLastElementId());
                mockupGraph.addEdge(moe, moe.getStart(), moe.getEnd());

                ArrayList<String> reqList = new ArrayList<String>();
                for (OutTokenRequirement otr : outEdge.getTokenRequirements()) {
                    reqList.add(otr.toString());
                }
                moe.setMockupTokenRequirements(reqList);
            }
        }

        this.graph = mockupGraph;
        this.locations = mockupLocations;
    }

    private void writeObject(ObjectOutputStream os) {
        try {
            os.defaultWriteObject();

        } catch (IOException ex) {
            Logger.getLogger(ReflectedEventSpecification.class
                    .getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        try {
            ois.defaultReadObject();
            // Populate transient fields
            getTransientGraph();
            getLayout();
        } catch (IOException e) {
            LOGGER.severe("IO Exception in MissionPlanSpecification readObject");
            throw e;
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Class Not Found Exception in MissionPlanSpecification readObject");
            throw e;
        }
    }
}
