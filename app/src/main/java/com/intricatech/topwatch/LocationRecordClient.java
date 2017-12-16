package com.intricatech.topwatch;

import android.location.Location;

/**
 * Created by Bolgbolg on 17/11/2017.
 */

public interface LocationRecordClient {

    void setTotalDistance(double totalDistanceTravelled);

    void setAccuracy(double accuracy);

    void updateMapWithLocationOnly(Location location, boolean addToPolyline);

}
