package com.tomtom.buster.navsdkextensions.model

import com.tomtom.sdk.location.GeoPoint

interface BusRepositoryInterface {
    val allBusStops: List<BusStop>
    val allBusLines: List<BusLine>

    fun getLinesForStop(stop: BusStop): List<BusLine>

    fun getNearestStops(
        location: GeoPoint,
        numberOfStops: Number,
    ): List<BusStop>
}
