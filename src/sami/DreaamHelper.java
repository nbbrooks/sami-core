package sami;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import java.awt.geom.Point2D;
import sami.gui.GuiConfig;
import sami.mission.Edge;
import sami.mission.Vertex;

/**
 *
 * @author nbb
 */
public class DreaamHelper {

    // Length of grid segment for "snapping" vertices
    public static final int GRID_LENGTH = 50;

    /**
     * Helps organize the graph visualization by snapping the point to a grid
     * structure
     *
     * @param point The point to snap to the grid
     * @return The snapped point
     */
    public static Point snapToGrid(Point point) {
        Point gridPoint = new Point((int) (point.x / GRID_LENGTH + 0.5) * GRID_LENGTH, (int) (point.y / GRID_LENGTH + 0.5) * GRID_LENGTH);
        return gridPoint;
    }

    /**
     * Helps organize the graph visualization by snapping the point to a grid
     * structure
     *
     * @param point The point to snap to the grid
     * @return The snapped point
     */
    public static Point2D snapToGrid(Point2D point) {
        Point2D.Double gridPoint = new Point2D.Double((int) (point.getX() / GRID_LENGTH + 0.5) * GRID_LENGTH, (int) (point.getY() / GRID_LENGTH + 0.5) * GRID_LENGTH);
        return gridPoint;
    }

    /**
     * Get the nearest grid-snapped point in a particular direction(s) which has
     * no vertex on it
     *
     * @param vv The graph visualization to search
     * @param x The starting x position in the graph
     * @param y The starting y position in the graph
     * @param searchDirections The order and list of directions to search: [0,
     * 7] representing N to NW
     * @return A grid snapped point with no vertex on it
     */
    public static Point getVertexFreePoint(VisualizationViewer<Vertex, Edge> vv, double x, double y, int[] searchDirections) {
        if (searchDirections == null) {
            searchDirections = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        }
        // First snap the provided point to our grid
        Point startGridPoint = snapToGrid(new Point((int) x, (int) y));
        x = startGridPoint.x;
        y = startGridPoint.y;
        GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
        // Check to see if starting point is free
        Vertex vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y);
        if (vertex == null || vertex.getVisibilityMode() == GuiConfig.VisibilityMode.None) {
            return new Point((int) x, (int) y);
        }
        int multiplier = 1;
        double newX = 0, newY = 0;
        while (true) {
            for (int curDirection = 0; curDirection < searchDirections.length; curDirection++) {
                switch (searchDirections[curDirection]) {
                    case (0): // N
                        newX = x;
                        newY = y - multiplier * GRID_LENGTH;
                        break;
                    case (1): // NE
                        newX = x + multiplier * GRID_LENGTH;
                        newY = y - multiplier * GRID_LENGTH;
                        break;
                    case (2): // E
                        newX = x + multiplier * GRID_LENGTH;
                        newY = y;
                        break;
                    case (3): // SE
                        newX = x + multiplier * GRID_LENGTH;
                        newY = y + multiplier * GRID_LENGTH;
                        break;
                    case (4): // S
                        newX = x;
                        newY = y + multiplier * GRID_LENGTH;
                        break;
                    case (5): // SW
                        newX = x - multiplier * GRID_LENGTH;
                        newY = y + multiplier * GRID_LENGTH;
                        break;
                    case (6): // W
                        newX = x - multiplier * GRID_LENGTH;
                        newY = y;
                        break;
                    case (7): // NW
                        newX = x - multiplier * GRID_LENGTH;
                        newY = y - multiplier * GRID_LENGTH;
                        break;
                }
                vertex = pickSupport.getVertex(vv.getGraphLayout(), newX, newY);
                if (vertex == null || vertex.getVisibilityMode() == GuiConfig.VisibilityMode.None) {
                    return new Point((int) newX, (int) newY);
                }
            }
            multiplier++;
        }
    }

    /**
     * Get the nearest grid-snapped point which has no vertex on it
     *
     * @param vv The graph visualization to search
     * @param x The starting x position in the graph
     * @param y The starting y position in the graph
     * @return A grid snapped point with no vertex on it
     */
    public static Point getVertexFreePoint(VisualizationViewer<Vertex, Edge> vv, double x, double y) {
        return getVertexFreePoint(vv, x, y, null);
    }

    /**
     * Get the nearest vertex in a particular direction(s) to the provided
     * location
     *
     * @param vv The graph visualization to search
     * @param x The starting x position in the graph
     * @param y The starting y position in the graph
     * @param maxSearchDistance The maximum Manhattan distance to look for a
     * vertex in
     * @param vertexVisible Whether the vertex must be visible to the user
     * (true) or could be a vertex hidden from the user (false)
     * @param searchDirections The order and list of directions to search: [0,
     * 7] representing N to NW
     * @return The nearest vertex to the provided location
     */
    public static Vertex getNearestVertex(VisualizationViewer<Vertex, Edge> vv, double x, double y, double maxSearchDistance, boolean vertexVisible, int[] searchDirections) {
        if (searchDirections == null) {
            searchDirections = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        }
        GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
        Vertex vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y);
        for (int r = 1; r <= maxSearchDistance && vertex == null; r++) {
            for (int curDirection = 0; curDirection < searchDirections.length && vertex == null; curDirection++) {
                switch (searchDirections[curDirection]) {
                    case (0): // N
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y - r);
                        break;
                    case (1): // NE
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x + r, y - r);
                        break;
                    case (2): // E
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x + r, y);
                        break;
                    case (3): // SE
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x + r, y + r);
                        break;
                    case (4): // S
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y + r);
                        break;
                    case (5): // SW
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x - r, y + r);
                        break;
                    case (6): // W
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x - r, y);
                        break;
                    case (7): // NW
                        vertex = pickSupport.getVertex(vv.getGraphLayout(), x - r, y - r);
                        break;
                }
                if (vertexVisible && vertex != null && vertex.getVisibilityMode() == GuiConfig.VisibilityMode.None) {
                    vertex = null;
                }
            }
        }
        return vertex;
    }

    /**
     * Get the nearest user-visible vertex to the provided location
     *
     * @param vv The graph visualization to search
     * @param x The starting x position in the graph
     * @param y The starting y position in the graph
     * @param maxSearchDistance The maximum Manhattan distance to look for a
     * vertex in
     * @return The nearest vertex to the provided location
     */
    public static Vertex getNearestVertex(VisualizationViewer<Vertex, Edge> vv, double x, double y, double maxSearchDistance) {
        return getNearestVertex(vv, x, y, maxSearchDistance, true, null);
    }

    /**
     * Get the nearest edge in a particular direction(s) to the provided
     * location
     *
     * @param vv The graph visualization to search
     * @param x The starting x position in the graph
     * @param y The starting y position in the graph
     * @param maxSearchDistance The maximum Manhattan distance to look for a
     * edge in
     * @param edgeVisible Whether the edge must be visible to the user (true) or
     * could be a edge hidden from the user (false)
     * @param searchDirections The order and list of directions to search: [0,
     * 7] representing N to NW
     * @return The nearest edge to the provided location
     */
    public static Edge getNearestEdge(VisualizationViewer<Vertex, Edge> vv, double x, double y, int maxSearchDistance, boolean edgeVisible, int[] searchDirections) {
        if (searchDirections == null) {
            searchDirections = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        }
        GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
        Edge edge = pickSupport.getEdge(vv.getGraphLayout(), x, y);
        for (int r = 1; r <= maxSearchDistance && edge == null; r++) {
            for (int curDirection = 0; curDirection < searchDirections.length && edge == null; curDirection++) {
                switch (curDirection) {
                    case (0): // N
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x, y - r);
                        break;
                    case (1): // NE
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x + r, y - r);
                        break;
                    case (2): // E
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x + r, y);
                        break;
                    case (3): // SE
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x + r, y + r);
                        break;
                    case (4): // S
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x, y + r);
                        break;
                    case (5): // SW
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x - r, y + r);
                        break;
                    case (6): // W
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x - r, y);
                        break;
                    case (7): // NW
                        edge = pickSupport.getEdge(vv.getGraphLayout(), x - r, y - r);
                        break;
                }
                if (edgeVisible && edge != null && edge.getVisibilityMode() == GuiConfig.VisibilityMode.None) {
                    edge = null;
                }
            }
        }
        return edge;
    }

    /**
     * Get the nearest user-visible edge to the provided location
     *
     * @param vv The graph visualization to search
     * @param x The starting x position in the graph
     * @param y The starting y position in the graph
     * @param maxSearchDistance The maximum Manhattan distance to look for a
     * edge in
     * @return The nearest edge to the provided location
     */
    public static Edge getNearestEdge(VisualizationViewer<Vertex, Edge> vv, double x, double y, int maxSearchDistance) {
        return getNearestEdge(vv, x, y, maxSearchDistance, true, null);
    }
}
