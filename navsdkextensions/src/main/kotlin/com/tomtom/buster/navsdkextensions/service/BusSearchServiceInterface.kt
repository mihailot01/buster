package com.tomtom.buster.navsdkextensions.service

import com.tomtom.buster.navsdkextensions.model.BusRide
import com.tomtom.sdk.location.GeoPoint

interface BusSearchServiceInterface {
    /**
     * Provides the direct bus line that connects the origin and destination points.
     *
     * @param origin The origin point.
     * @param destination The destination point.
     * @return The bus ride which contains bus line, origin and destination bus stops.
     */
    fun search(
        origin: GeoPoint,
        destination: GeoPoint,
    ): BusRide
}
