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

    public static Point snapToGrid(Point point) {
        Point gridPoint = new Point((int) (point.x / GRID_LENGTH + 0.5) * GRID_LENGTH, (int) (point.y / GRID_LENGTH + 0.5) * GRID_LENGTH);
        return gridPoint;
    }
    
    public static Point2D snapToGrid(Point2D point) {
        Point2D.Double gridPoint = new Point2D.Double((int) (point.getX() / GRID_LENGTH + 0.5) * GRID_LENGTH, (int) (point.getY() / GRID_LENGTH + 0.5) * GRID_LENGTH);
        return gridPoint;
    }

    public static Point getVertexFreePoint(VisualizationViewer<Vertex, Edge> vv, double x, double y, double searchRadius) {
        Point freePoint = new Point((int) x, (int) y);
        while (getNearestVertex(vv, freePoint.getX(), freePoint.getY(), searchRadius) != null) {
            freePoint.setLocation(freePoint.getX() - (2 * searchRadius + 1), freePoint.getY() - (2 * searchRadius + 1));
        }
        return freePoint;
    }

    public static Vertex getNearestVertex(VisualizationViewer<Vertex, Edge> vv, double x, double y, double radius) {
        return getNearestVertex(vv, x, y, radius, true);
    }

    public static Vertex getNearestVertex(VisualizationViewer<Vertex, Edge> vv, double x, double y, double radius, boolean vertexVisible) {
        GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
        Vertex vertex = pickSupport.getVertex(vv.getGraphLayout(), x, y);
        for (int r = 1; r <= radius && vertex == null; r++) {
            for (int dir = 0; dir < 8 && vertex == null; dir++) {
                switch (dir) {
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

    public static Edge getNearestEdge(VisualizationViewer<Vertex, Edge> vv, double x, double y, int radius) {
        return getNearestEdge(vv, x, y, radius, true);
    }

    public static Edge getNearestEdge(VisualizationViewer<Vertex, Edge> vv, double x, double y, int radius, boolean edgeVisible) {
        GraphElementAccessor<Vertex, Edge> pickSupport = vv.getPickSupport();
        Edge edge = pickSupport.getEdge(vv.getGraphLayout(), x, y);
        for (int r = 1; r <= radius && edge == null; r++) {
            for (int dir = 0; dir < 8 && edge == null; dir++) {
                switch (dir) {
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
}
