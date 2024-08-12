package com.tomtom.buster.navsdkextensions

import com.tomtom.buster.navsdkextensions.navigation.BusRouteReplanner
import com.tomtom.quantity.Angle
import com.tomtom.quantity.Distance
import com.tomtom.quantity.Probability
import com.tomtom.quantity.Speed
import com.tomtom.sdk.annotations.InternalTomTomSdkApi
import com.tomtom.sdk.common.Result
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
import com.tomtom.sdk.navigation.locationcontext.LocationContext
import com.tomtom.sdk.navigation.mapmatching.MapMatchingResult
import com.tomtom.sdk.navigation.mapmatching.MatchedLocation
import com.tomtom.sdk.navigation.replanning.RouteReplanningFailure
import com.tomtom.sdk.navigation.routereplanner.RouteReplanner
import com.tomtom.sdk.navigation.routereplanner.RouteReplannerResponse
import com.tomtom.sdk.navigation.routereplanner.online.OnlineRouteReplannerFactory
import com.tomtom.sdk.routing.RoutePlanner
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
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import kotlin.time.Duration

class BusRouteReplannerTest {
    private lateinit var busRouteReplanner: BusRouteReplanner
    private lateinit var defaultRouteReplanner: RouteReplanner
    private lateinit var routeReplannerBusPlanner: RouteReplanner

    @OptIn(InternalTomTomSdkApi::class)
    private fun givenLocationSnapshot(): LocationSnapshot {
        val geoLocation = GeoLocation(GeoPoint(44.0, 44.0), elapsedRealtimeNanos = 0)
        val matchedLocation = MatchedLocation(geoLocation, 10, Probability.percent(1), false, Angle.ZERO)
        val mapMatchingResult = MapMatchingResult(matchedLocation, geoLocation)
        val locationContext = LocationContext(Speed.ZERO)
        return LocationSnapshot(geoLocation, mapMatchingResult, locationContext)
    }

    @OptIn(InternalTomTomSdkApi::class)
    private fun givenNavigationSnapshot() =
        NavigationSnapshot(
            givenLocationSnapshot(),
            DrivingHistorySnapshot(),
            ConfigurationSnapshot(),
            Vehicle.Car(),
            givenTripSnapshot(),
        )

    @OptIn(InternalTomTomSdkApi::class)
    private fun givenTripSnapshot(): TripSnapshot {
        val routePlan = RoutePlan(ROUTE, ROUTE_PLANNING_OPTIONS)
        val routeSnapshot = RouteSnapshot(routePlan)
        return TripSnapshot(routeSnapshot)
    }

    @Before
    fun setup() {
        defaultRouteReplanner = mockk()
        routeReplannerBusPlanner = mockk()
        val routePlanner = mockk<RoutePlanner>()
        val busRoutePlanner = mockk<RoutePlanner>()
        mockkStatic(OnlineRouteReplannerFactory::class)
        every { OnlineRouteReplannerFactory.create(routePlanner) } returns defaultRouteReplanner
        every { OnlineRouteReplannerFactory.create(busRoutePlanner) } returns routeReplannerBusPlanner
        every { defaultRouteReplanner.close() } returns Unit
        every { routeReplannerBusPlanner.close() } returns Unit
        busRouteReplanner = BusRouteReplanner.create(busRoutePlanner, routePlanner)
    }

    @Test
    fun `back to route provide new bus route and is always invoked with positive hasDeviated`() {
        val navigationSnapshot = givenNavigationSnapshot()
        assertFalse(navigationSnapshot.tripSnapshot!!.hasDeviated)
        val navigationSnapshotSlot = slot<NavigationSnapshot>()
        every { routeReplannerBusPlanner.backToRoute(capture(navigationSnapshotSlot)) } returns mockk()
        busRouteReplanner.backToRoute(navigationSnapshot)
        assertTrue(navigationSnapshotSlot.captured.tripSnapshot!!.hasDeviated)
        verify { routeReplannerBusPlanner.backToRoute(any()) }
    }

    @Test
    fun `increment route behaves as DefaultRouteReplanner increment route`() {
        val expectedResult = mockk<Result<RouteReplannerResponse, RouteReplanningFailure>>()
        val navigationSnapshot = mockk<NavigationSnapshot>()
        every { defaultRouteReplanner.incrementRouteContents(navigationSnapshot) } returns expectedResult
        assertEquals(expectedResult, busRouteReplanner.incrementRouteContents(navigationSnapshot))
    }

    @Test
    fun `update calls defaultRouteReplanner update`() {
        val navigationSnapshot = givenNavigationSnapshot()
        val navigationSnapshotSlot = slot<NavigationSnapshot>()
        every { defaultRouteReplanner.update(capture(navigationSnapshotSlot)) } returns mockk()
        busRouteReplanner.update(navigationSnapshot)
        assertNotEquals(navigationSnapshot, navigationSnapshotSlot.captured)
        assertEquals(navigationSnapshotSlot.captured.tripSnapshot!!.activeRoute.routePlan.routePlanningOptions.itinerary.waypoints.size, 1)
        verify { defaultRouteReplanner.update(any()) }
    }

    @Test
    fun `close behaves as DefaultRouteReplanner close`() {
        busRouteReplanner.close()
        verify { defaultRouteReplanner.close() }
        verify { routeReplannerBusPlanner.close() }
    }

    @After
    fun cleanup() {
        busRouteReplanner.close()
    }

    private companion object {
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
                            Duration.ZERO,
                            departureTimeWithZone = Calendar.getInstance(),
                            arrivalTimeWithZone = Calendar.getInstance(),
                        ),
                    ),
                    RouteLeg(
                        listOf(GeoPoint(0.01, 0.01)),
                        emptyList(),
                        Summary(
                            Distance.meters(10),
                            Duration.ZERO,
                            departureTimeWithZone = Calendar.getInstance(),
                            arrivalTimeWithZone = Calendar.getInstance(),
                        ),
                    ),
                ),
                listOf(
                    RouteStop(RouteStopId(), Place(GeoPoint(0.0, 0.0)), listOf(GeoPoint(0.0, 0.0)), Distance.ZERO),
                    RouteStop(RouteStopId(), Place(GeoPoint(0.01, 0.01)), listOf(GeoPoint(0.01, 0.01)), Distance.ZERO),
                    RouteStop(RouteStopId(), Place(GeoPoint(0.0, 0.0)), listOf(GeoPoint(0.0, 0.0)), Distance.ZERO),
                ),
                Sections(),
                RouteModificationHistory(RouteTimestamp(0, Calendar.getInstance())),
            )
        private val ROUTE_PLANNING_OPTIONS = RoutePlanningOptions(Itinerary(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0)))
    }
}
