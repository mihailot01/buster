
package com.tomtom.buster.navsdkextensions

import com.tomtom.buster.navsdkextensions.model.BusLine
import com.tomtom.buster.navsdkextensions.model.BusRide
import com.tomtom.buster.navsdkextensions.model.BusStop
import com.tomtom.buster.navsdkextensions.routing.BusRoutePlanner
import com.tomtom.buster.navsdkextensions.service.BusSearchService
import com.tomtom.sdk.common.Result
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.guidance.RouteIncrementOptions
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.vehicle.Vehicle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BusRoutePlannerTest {
    private lateinit var busRoutePlanner: BusRoutePlanner
    private lateinit var routePlanner: RoutePlanner
    private val routePlanningOptionsSlot = slot<RoutePlanningOptions>()

    @Before
    fun setup() {
        routePlanner = mockk()
        val busSearchService = mockk<BusSearchService>()
        mockkObject(OnlineRoutePlanner.Companion)
        every { OnlineRoutePlanner.create(any(), any()) } returns routePlanner
        every { routePlanner.close() } returns Unit
        every { routePlanner.planRoute(capture(routePlanningOptionsSlot)) } returns mockk()
        every { routePlanner.planRoute(capture(routePlanningOptionsSlot), any()) } returns mockk()
        every { busSearchService.search(any(), any()) } returns busRide
        busRoutePlanner = BusRoutePlanner(routePlanner, busSearchService)
    }

    @Test
    fun `advanced guidance behave as OnlineRoutePlanner advanced guidance`() {
        val expectedResult = mockk<Result<Route, RoutingFailure>>()
        val routeIncrementOptions = mockk<RouteIncrementOptions>()
        every { routePlanner.advanceGuidanceProgress(routeIncrementOptions) } returns expectedResult
        assertEquals(expectedResult, busRoutePlanner.advanceGuidanceProgress(routeIncrementOptions))
    }

    @Test
    fun `plan route is called with correct itinerary with bus stop and line info`() {
        busRoutePlanner.planRoute(routePlanningOptions)
        val capturedItinerary = routePlanningOptionsSlot.captured.itinerary
        assertEquals(finalItinerary.waypoints.size, capturedItinerary.waypoints.size)
        assertEquals(finalItinerary.origin, capturedItinerary.origin)
        assertEquals(finalItinerary.destination, capturedItinerary.destination)
        for ((expectedWaypoint, capturedWaypoint) in finalItinerary.waypoints.zip(capturedItinerary.waypoints)) {
            assertEquals(expectedWaypoint.place.coordinate, capturedWaypoint.place.coordinate)
            assertTrue(capturedWaypoint.place.name.contains("stop:"))
            assertTrue(capturedWaypoint.place.name.contains("bus:"))
        }
    }

    @Test
    fun `plan route (with callback) is called with correct itinerary with bus stop and line info`() {
        busRoutePlanner.planRoute(routePlanningOptions, mockk())
        val capturedItinerary = routePlanningOptionsSlot.captured.itinerary
        assertEquals(finalItinerary.waypoints.size, capturedItinerary.waypoints.size)
        assertEquals(finalItinerary.origin, capturedItinerary.origin)
        assertEquals(finalItinerary.destination, capturedItinerary.destination)
        for ((expectedWaypoint, capturedWaypoint) in finalItinerary.waypoints.zip(capturedItinerary.waypoints)) {
            assertEquals(expectedWaypoint.place.coordinate, capturedWaypoint.place.coordinate)
            assertTrue(capturedWaypoint.place.name.contains("stop:"))
            assertTrue(capturedWaypoint.place.name.contains("bus:"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `plan route with custom waypoint`() {
        val routePlanningOptions =
            RoutePlanningOptions(
                itinerary = finalItinerary,
            )
        busRoutePlanner.planRoute(routePlanningOptions)
    }

    @After
    fun cleanup() {
        busRoutePlanner.close()
    }

    companion object {
        private val origin = GeoPoint(1.2345, 2.3456)
        private val destination = GeoPoint(3.4567, 4.5678)
        private val busStops =
            listOf(
                BusStop("0", "Stop 0", GeoPoint(0.0, 0.0)),
                BusStop("1", "Stop 1", GeoPoint(1.0, 1.0)),
                BusStop("2", "Stop 2", GeoPoint(2.0, 2.0)),
                BusStop("3", "Stop 3", GeoPoint(3.0, 3.0)),
                BusStop("4", "Stop 4", GeoPoint(4.0, 4.0)),
                BusStop("5", "Stop 5", GeoPoint(5.0, 5.0)),
            )
        private val busLine =
            BusLine("0-5", "Line 4", listOf(busStops[0], busStops[1], busStops[2], busStops[3], busStops[4], busStops[5]))
        private val busRide = BusRide(busLine, busStops.subList(2, 5))
        private val routePlanningOptions =
            RoutePlanningOptions(
                itinerary =
                    Itinerary(
                        origin = origin,
                        destination = destination,
                        waypoints = emptyList(),
                    ),
                vehicle = Vehicle.Car(),
            )
        private val finalItinerary =
            Itinerary(
                origin = origin,
                destination = destination,
                waypoints = listOf(busStops[2].location, busStops[3].location, busStops[4].location),
            )
    }
}
