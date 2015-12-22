package sami.path;

import java.io.Serializable;

public class SimpleLatLon implements Serializable {

    public double latitude, longitude;

    public SimpleLatLon() {
    }

    public SimpleLatLon(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public SimpleLatLon clone() {
        SimpleLatLon clone = new SimpleLatLon(latitude, longitude);
        return clone;
    }

    public String toString() {
        return "Location: [" + latitude + ", " + longitude + "]";
    }
}
