package com.tomtom.buster.navsdkextensions.navigation

import com.tomtom.sdk.navigation.NavigationSnapshot
import com.tomtom.sdk.navigation.arrival.ArrivalDetectionEngine
import com.tomtom.sdk.navigation.arrival.ArrivalDetectionEngineOptions
import com.tomtom.sdk.navigation.arrival.DefaultArrivalDetectionEngineFactory
import com.tomtom.sdk.navigation.arrival.DestinationArrivalStatus
import com.tomtom.sdk.navigation.arrival.WaypointStatus

/**
 * Determines the arrival status of the route destination, the next waypoint during bus navigation or end status of bus drive.
 */
class BusArrivalDetectionEngine internal constructor(private val arrivalDetectionEngine: ArrivalDetectionEngine) : ArrivalDetectionEngine {
    override fun hasArrivedAtDestination(navigationSnapshot: NavigationSnapshot): DestinationArrivalStatus {
        return arrivalDetectionEngine.hasArrivedAtDestination(navigationSnapshot)
    }

    override fun hasArrivedAtWaypoint(navigationSnapshot: NavigationSnapshot): WaypointStatus {
        return arrivalDetectionEngine.hasArrivedAtWaypoint(navigationSnapshot)
    }

    companion object {
        /**
         * Creates a new instance of [BusArrivalDetectionEngine].
         * Only one instance of [BusArrivalDetectionEngine] should be created per application.
         */
        fun create(engineOptions: ArrivalDetectionEngineOptions = ArrivalDetectionEngineOptions()): BusArrivalDetectionEngine {
            return BusArrivalDetectionEngine(DefaultArrivalDetectionEngineFactory.create(engineOptions))
        }
    }
}
