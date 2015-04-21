
package sami.map;

import java.io.Serializable;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class ViewPoint extends Location implements Serializable {


    public String toString() {
        return "ViewPoint: [" + getCoordinate() + ", " + getAltitude() + "]";
    }
}
