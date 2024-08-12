package com.tomtom.buster.navsdkextensions

import com.tomtom.buster.navsdkextensions.engine.BusGuidanceEngine
import com.tomtom.quantity.Angle
import com.tomtom.quantity.Distance
import com.tomtom.quantity.Probability
import com.tomtom.quantity.Speed
import com.tomtom.sdk.annotations.InternalTomTomSdkApi
import com.tomtom.sdk.common.Context
import com.tomtom.sdk.common.UniqueId
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.Place
import com.tomtom.sdk.navigation.ConfigurationSnapshot
import com.tomtom.sdk.navigation.DrivingHistorySnapshot
import com.tomtom.sdk.navigation.LocationSnapshot
import com.tomtom.sdk.navigation.NavigationSnapshot
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.RouteSnapshot
import com.tomtom.sdk.navigation.TripSnapshot
import com.tomtom.sdk.navigation.guidance.Guidance
import com.tomtom.sdk.navigation.guidance.GuidanceEngine
import com.tomtom.sdk.navigation.guidance.GuidanceEngineFactory
import com.tomtom.sdk.navigation.guidance.InstructionPhase
import com.tomtom.sdk.navigation.guidance.instruction.WaypointGuidanceInstruction
import com.tomtom.sdk.navigation.locationcontext.LocationContext
import com.tomtom.sdk.navigation.mapmatching.MapMatchingResult
import com.tomtom.sdk.navigation.mapmatching.MatchedLocation
import com.tomtom.sdk.navigation.progress.RouteProgress
import com.tomtom.sdk.navigation.progress.RouteStopProgress
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.routing.route.RouteId
import com.tomtom.sdk.routing.route.RouteLeg
import com.tomtom.sdk.routing.route.RouteModificationHistory
import com.tomtom.sdk.routing.route.RouteStop
import com.tomtom.sdk.routing.route.RouteStopId
import com.tomtom.sdk.routing.route.RouteTimestamp
import com.tomtom.sdk.routing.route.Summary
import com.tomtom.sdk.routing.route.section.Sections
import com.tomtom.sdk.vehicle.Vehicle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import kotlin.time.Duration

class BusGuidanceEngineTest {
    private val defaultGuidanceEngineMock = mockk<GuidanceEngine>(relaxed = true)
    private val guidanceMock = givenMockGuidance()

    @Before
    fun setup() {
        mockkStatic(GuidanceEngineFactory::class)
        every { defaultGuidanceEngineMock.generateGuidance(any()) } returns guidanceMock
        every {
            GuidanceEngineFactory.createDynamicGuidanceEngine(
                any(),
                any(),
            )
        } returns defaultGuidanceEngineMock
    }

    @Test
    fun `default guidance is returned if not on bus leg`() {
        val busGuidanceEngineTested = BusGuidanceEngine.create(mockk<Context>())

        val returnedGuidance = busGuidanceEngineTested.generateGuidance(givenNavigationSnapshot(0))

        assertEquals(returnedGuidance, guidanceMock)
    }

    @Test
    fun `custom guidance is returned if on bus leg`() {
        val busGuidanceEngineTested = BusGuidanceEngine.create(mockk<Context>())

        val returnedGuidance = busGuidanceEngineTested.generateGuidance(givenNavigationSnapshot(1))

        assertNotEquals(returnedGuidance, guidanceMock)
        assertEquals(returnedGuidance.instructions.size, 1)
        assertEquals(returnedGuidance.instructions.first() is WaypointGuidanceInstruction, true)
    }

    @OptIn(InternalTomTomSdkApi::class)
    private fun givenNavigationSnapshot(numberOfVisitedWaypoints: Int) =
        NavigationSnapshot(
            givenLocationSnapshot(),
            DrivingHistorySnapshot(),
            ConfigurationSnapshot(),
            Vehicle.Car(),
            givenTripSnapshot(numberOfVisitedWaypoints),
        )

    @OptIn(InternalTomTomSdkApi::class)
    private fun givenLocationSnapshot(): LocationSnapshot =
        LocationSnapshot(
            rawLocation = givenGeoLocation(),
            mapMatchingResult =
                MapMatchingResult(
                    MatchedLocation(givenGeoLocation(), 10, Probability.percent(1), false, Angle.ZERO),
                    givenGeoLocation(),
                ),
            locationContext = LocationContext(Speed.ZERO),
        )

    private fun givenGeoLocation() = GeoLocation(WAYPOINTS.first(), elapsedRealtimeNanos = 1)

    @OptIn(InternalTomTomSdkApi::class)
    private fun givenTripSnapshot(numberOfVisitedWaypoints: Int) =
        TripSnapshot(
            RouteSnapshot(
                routePlan = RoutePlan(ROUTE, ROUTE_PLANNING_OPTIONS),
                numberOfVisitedWaypoints = numberOfVisitedWaypoints,
                nextWaypoint = ROUTE.waypoints.first(),
                routeProgress = givenRouteProgress(numberOfVisitedWaypoints),
            ),
        )

    private fun givenRouteProgress(numberOfVisitedWaypoints: Int) =
        RouteProgress(
            Distance.ZERO,
            ROUTE.waypoints.drop(numberOfVisitedWaypoints).map { RouteStopProgress(it.id, Duration.ZERO, Distance.ZERO) },
        )

    private fun givenMockGuidance() =
        Guidance(
            instructions = listOf(),
            announcement = null,
            distanceToManeuver = Distance.meters(0),
            currentPhase = InstructionPhase.Early,
        )

    private companion object {
        private val ORIGIN = GeoPoint(0.0, 0.0)
        private val DESTINATION = GeoPoint(0.3, 0.3)
        private val WAYPOINTS = listOf(GeoPoint(0.1, 0.1), GeoPoint(0.02, 0.02))
        private var time =
            Calendar.getInstance().let {
                it.add(Calendar.SECOND, 1)
                it
            }
        private val ROUTE =
            Route(
                RouteId(UniqueId()),
                Summary(
                    Distance.meters(10),
                    Duration.ZERO,
                    departureTimeWithZone = Calendar.getInstance(),
                    arrivalTimeWithZone = Calendar.getInstance(),
                ),
                listOf(
                    RouteLeg(
                        listOf(GeoPoint(0.0, 0.0)),
                        emptyList(),
                        Summary(
                            Distance.meters(10),
                            Duration.INFINITE,
                            departureTimeWithZone = time,
                            arrivalTimeWithZone = time,
                        ),
                    ),
                    RouteLeg(
                        listOf(GeoPoint(0.1, 0.1)),
                        emptyList(),
                        Summary(
                            Distance.meters(10),
                            Duration.INFINITE,
                            departureTimeWithZone = time,
                            arrivalTimeWithZone = time,
                        ),
                    ),
                    RouteLeg(
                        listOf(GeoPoint(0.2, 0.2)),
                        emptyList(),
                        Summary(
                            Distance.meters(10),
                            Duration.INFINITE,
                            departureTimeWithZone = time,
                            arrivalTimeWithZone = time,
                        ),
                    ),
                ),
                listOf(
                    RouteStop(
                        RouteStopId(),
                        Place(GeoPoint(0.0, 0.0), name = "stop: stop bus: bus"),
                        listOf(GeoPoint(0.0, 0.0)),
                        Distance.ZERO,
                    ),
                    RouteStop(
                        RouteStopId(),
                        Place(GeoPoint(0.1, 0.1), name = "stop: stop bus: bus"),
                        listOf(GeoPoint(0.1, 0.1)),
                        Distance.ZERO,
                    ),
                    RouteStop(
                        RouteStopId(),
                        Place(GeoPoint(0.2, 0.2), name = "stop: stop bus: bus"),
                        listOf(GeoPoint(0.2, 0.2)),
                        Distance.ZERO,
                    ),
                    RouteStop(
                        RouteStopId(),
                        Place(GeoPoint(0.3, 0.3), name = "stop: stop bus: bus"),
                        listOf(GeoPoint(0.3, 0.3)),
                        Distance.ZERO,
                    ),
                ),
                Sections(),
                RouteModificationHistory(RouteTimestamp(0, Calendar.getInstance())),
            )
        private val ROUTE_PLANNING_OPTIONS = RoutePlanningOptions(Itinerary(ORIGIN, DESTINATION, WAYPOINTS))
    }
}
