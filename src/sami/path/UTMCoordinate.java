package sami.path;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 *
 * @author pscerri
 */
public class UTMCoordinate implements Serializable {

    public enum Hemisphere {NORTH, SOUTH};
    double northing = 0.0;
    double easting = 0.0;
    String zone = "";
    Hemisphere hemisphere;
    
    transient private static CoordinateConversion cvt = new CoordinateConversion();

    public UTMCoordinate() {
    }

    public UTMCoordinate(double latitude, double longitude) {
        String s = cvt.latLon2UTM(latitude, longitude);
        StringTokenizer tokenizer = new StringTokenizer(s);
        if (tokenizer.countTokens() == 4) {
            zone = tokenizer.nextToken();
            // Zones [A, M] are southern and zones [N, Z] are northern
            String zoneLetter = tokenizer.nextToken();
            zone += zoneLetter;
            hemisphere = ((int) zoneLetter.charAt(0)) - ((int) 'N') >= 0 ? Hemisphere.NORTH : Hemisphere.SOUTH;
            easting = Double.parseDouble(tokenizer.nextToken());
            northing = Double.parseDouble(tokenizer.nextToken());
        }
    }

    public UTMCoordinate(double easting, double northing, String zone) {
        this.easting = easting;
        this.northing = northing;
        this.zone = zone;
        hemisphere = ((int) zone.charAt(zone.length() - 1)) - ((int) 'N') >= 0 ? Hemisphere.NORTH : Hemisphere.SOUTH;
    }

    public double getNorthing() {
        return northing;
    }

    public void setNorthing(double northing) {
        this.northing = northing;
    }

    public double getEasting() {
        return easting;
    }

    public void setEasting(double easting) {
        this.easting = easting;
    }

    public String getZone() {
        return zone;
    }

    public char getZoneChar() {
        return zone.charAt(zone.length() - 1);
    }

    public int getZoneNumber() {
        return Integer.parseInt(zone.substring(0, zone.length() - 1));
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Hemisphere getHemisphere() {
        return hemisphere;
    }

    public void setHemisphere(Hemisphere hemisphere) {
        this.hemisphere = hemisphere;
    }

    public String toString() {
        return "UTMCoordinate: [" + easting + ", " + northing + ", " + zone + "]";
    }
}
