package com.intricatech.topwatch;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Created by Bolgbolg on 17/11/2017.
 */

public interface LocationRecordClient {

    void setLocationRecord(LocationRecord locationRecord);

    void setTotalDistance(double totalDistanceTravelled);

    void setSplitDistance(double splitDistance);

    void setAccuracy(double accuracy);

    void updateMapWithPolyline(PolylineOptions options);

    void updateMapWithLocationOnly(LatLng latLng);
}
