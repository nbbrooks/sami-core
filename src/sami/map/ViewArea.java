package sami.map;

import sami.path.Location;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nbb
 */
public class ViewArea implements Serializable {

    protected List<Location> points = new ArrayList<Location>();

    public ViewArea() {
    }

    public ViewArea(List<Location> points) {
        this.points = points;
    }

    public List<Location> getPoints() {
        return points;
    }

    public String toString() {
        return "MapCorners [" + (points != null ? points.toString() : "null");
    }
}
