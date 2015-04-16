package sami.environment;

import java.io.Serializable;
import java.util.ArrayList;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class EnvironmentProperties implements Serializable {

    static final long serialVersionUID = 2L;
    private boolean needsSaving = false;
//    private ArrayList<ArrayList<Location>> obstacleList = new ArrayList<ArrayList<Location>>();
    private Location defaultLocation;
//    private ArrayList<Marker> markers = new ArrayList<Marker>();
//    private ArrayList<Polyline> lines = new ArrayList<Polyline>();
//    private ArrayList<SurfacePolygon> areas = new ArrayList<SurfacePolygon>();
    private ArrayList<Location> markerPoints = new ArrayList<Location>();
    private ArrayList<ArrayList<Location>> linePoints = new ArrayList<ArrayList<Location>>();
    private ArrayList<ArrayList<Location>> areaPoints = new ArrayList<ArrayList<Location>>();
    
    
    public Location getDefaultLocation() {
        return defaultLocation;
    }
    
    public void setDefaultLocation(Location defaultLocation) {
        this.defaultLocation = defaultLocation;
    }
    
    public ArrayList<Location> getMarkerPoints() {
        return markerPoints;
    }
    
    public void setMarkerPoints(ArrayList<Location> markerPoints) {
        this.markerPoints = markerPoints;
        needsSaving = true;
    }
    
    public ArrayList<ArrayList<Location>> getLinePoints() {
        return linePoints;
    }
    
    public void setLinePoints(ArrayList<ArrayList<Location>> linePoints) {
        this.linePoints = linePoints;
        needsSaving = true;
    }
    
    public ArrayList<ArrayList<Location>> getAreaPoints() {
        return areaPoints;
    }
    
    public void setAreaPoints(ArrayList<ArrayList<Location>> areaPoints) {
        this.areaPoints = areaPoints;
        needsSaving = true;
    }

    public boolean needsSaving() {
        return needsSaving;
    }

    public void saved() {
        needsSaving = false;
    }
}
