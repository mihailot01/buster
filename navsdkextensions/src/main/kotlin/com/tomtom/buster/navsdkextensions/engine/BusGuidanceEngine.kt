package com.tomtom.buster.navsdkextensions.engine

import android.content.Context
import com.tomtom.buster.navsdkextensions.RouteLegDistinctor
import com.tomtom.quantity.Distance
import com.tomtom.sdk.common.UniqueId
import com.tomtom.sdk.location.DrivingSide
import com.tomtom.sdk.navigation.NavigationSnapshot
import com.tomtom.sdk.navigation.guidance.AnnouncementMode
import com.tomtom.sdk.navigation.guidance.Guidance
import com.tomtom.sdk.navigation.guidance.GuidanceEngine
import com.tomtom.sdk.navigation.guidance.GuidanceEngineFactory
import com.tomtom.sdk.navigation.guidance.GuidanceEngineOptions
import com.tomtom.sdk.navigation.guidance.LaneGuidanceUpdate
import com.tomtom.sdk.navigation.guidance.instruction.Road
import com.tomtom.sdk.navigation.guidance.instruction.WaypointGuidanceInstruction
import com.tomtom.sdk.routing.route.RouteStop
import com.tomtom.sdk.routing.route.instruction.common.ItineraryPointRelativePosition
import java.util.Locale

val RouteStop.busStationName: String get() = place.name.substring(0, place.name.indexOf("bus:")).replace("stop:", "").trim()
val RouteStop.busNumber: String get() = place.name.substring(place.name.indexOf("bus:")).replace("bus:", "").trim()

class BusGuidanceEngine internal constructor(val context: Context) : GuidanceEngine {
    override var announcementMode = AnnouncementMode.Compact
    override var announcementsEnabled = true
    override val availableLanguages: List<Locale> = listOf(Locale.ENGLISH)

    private val dynamicGuidanceEngine: GuidanceEngine =
        GuidanceEngineFactory.createDynamicGuidanceEngine(context, GuidanceEngineOptions())

    override fun generateGuidance(navigationSnapshot: NavigationSnapshot): Guidance {
        val guidance = dynamicGuidanceEngine.generateGuidance(navigationSnapshot)
        if (!RouteLegDistinctor.checkIfOnBusLeg(navigationSnapshot.tripSnapshot!!.activeRoute)) {
            return guidance
        }
        val activeRoute = navigationSnapshot.tripSnapshot!!.activeRoute

        val getOffStationRouteStop = activeRoute.routePlan.route.waypoints.last()
        val remainingRouteStopsProgress = activeRoute.routeProgress.remainingRouteStopsProgress
        val distanceToGetOffStation = remainingRouteStopsProgress.find { it.routeStopId == getOffStationRouteStop.id }!!.remainingDistance

        val instruction =
            WaypointGuidanceInstruction(
                id = UniqueId(),
                waypointSide = ItineraryPointRelativePosition.Ahead,
                routeOffset = activeRoute.routeProgress.distanceAlongRoute,
                maneuverPoint = getOffStationRouteStop.place.coordinate,
                drivingSide = DrivingSide.RIGHT,
                combineWithNext = false,
                nextSignificantRoad =
                    Road(
                        name = activeRoute.nextWaypoint!!.busNumber,
                        numbers = listOf("get off at stop: ${getOffStationRouteStop.busStationName}"),
                        shields = emptyList(),
                    ),
            )

        return guidance.copy(
            instructions = listOf(instruction),
            distanceToManeuver = Distance.meters(distanceToGetOffStation.inMeters()),
        )
    }

    override fun generateLaneGuidance(navigationSnapshot: NavigationSnapshot): LaneGuidanceUpdate? {
        return dynamicGuidanceEngine.generateLaneGuidance(navigationSnapshot)
    }

    override fun close() {
        dynamicGuidanceEngine.close()
    }

    companion object {
        fun create(context: Context): BusGuidanceEngine {
            return BusGuidanceEngine(context)
        }
    }
}
