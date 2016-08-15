package sami.sensor;

import sami.path.Location;

/**
 *
 * @author nbb
 */
public class Observation {

    public final Location location;
    public final String variable;
    public final double value;
    public final long time;
    public final String source;
    
    
    public Observation (String variable, double value, String source, Location location, long time) {        
        this.variable = variable;
        this.value = value;
        this.source = source;
        this.location = location;
        this.time = time;
    }
    
    public String getVariable() {
        return variable;
    }

    public double getValue() {
        return value;
    }
    
    public String getSource() {
        return source;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public long getTime() {
        return time;
    }
    
    @Override
    public String toString() {
        return variable + ": " + value + " from " + source + " at " + location + " at " + time;
    }
}
