package sami.environment;

import java.io.Serializable;
import java.util.ArrayList;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class EnvironmentProperties implements Serializable {

    static final long serialVersionUID = 1L;
    private ArrayList<ArrayList<Location>> obstacleList = new ArrayList<ArrayList<Location>>();
    private Location defaultLocation;
    
    public ArrayList<ArrayList<Location>> getObstacleList() {
        return obstacleList;
    }
    
    public void setObstacleList(ArrayList<ArrayList<Location>> obstacleList) {
        this.obstacleList = obstacleList;
    }
    
    public Location getDefaultLocation() {
        return defaultLocation;
    }
    
    public void setDefaultLocation(Location defaultLocation) {
        this.defaultLocation = defaultLocation;
    }
}
