package com.intricatech.topwatch;

import static java.lang.Math.*;

/**
 * Created by Bolgbolg on 05/11/2017.
 *
 * Encapsulates a pair of GPS coordinates and provides utility methods for comparing them.
 */

public class GPSCoords {

    static final double EARTHS_RADIUS = 6371.0d;

    final double latitude, longitude;

    public GPSCoords(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Uses the Haversine formula.
     * @param coords1 First coordinate (order not important)
     * @param coords2 Second coordinate (order not important)
     * @return the distance in meters.
     */
    public static float getDistanceInMeters(GPSCoords coords1, GPSCoords coords2) {

        double deltaLat = toRadians(coords1.latitude - coords2.latitude);
        double deltaLong = toRadians(coords1.longitude - coords2.longitude);
        double lat1 = toRadians(coords1.latitude);
        double lat2 = toRadians(coords2.latitude);
        double temp1 = sin(deltaLat / 2) * sin(deltaLat / 2)
                + cos(lat1) * cos(lat2) * sin(deltaLong / 2) * sin(deltaLong / 2);
        double temp2 = 2 * atan2(sqrt(temp1), sqrt(1 - temp1));

        return ((float) (EARTHS_RADIUS * temp2));
    }
}
