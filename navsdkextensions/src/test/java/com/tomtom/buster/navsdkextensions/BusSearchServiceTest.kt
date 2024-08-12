package com.tomtom.buster.navsdkextensions

import com.tomtom.buster.navsdkextensions.model.BusLine
import com.tomtom.buster.navsdkextensions.model.BusRepositoryInterface
import com.tomtom.buster.navsdkextensions.model.BusStop
import com.tomtom.buster.navsdkextensions.service.BusSearchService
import com.tomtom.sdk.location.GeoPoint
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BusSearchServiceTest {
    private val busRepository: BusRepositoryInterface = mockk()
    private val busRoutingService = BusSearchService(busRepository)

    @Before
    fun setUp() {
        val busStops =
            listOf(
                BusStop("0", name = "Stop 0", location = GeoPoint(0.0, 0.0)),
                BusStop("1", name = "Stop 1", location = GeoPoint(1.0, 1.0)),
                BusStop("2", name = "Stop 2", location = GeoPoint(2.0, 2.0)),
                BusStop("3", name = "Stop 3", location = GeoPoint(3.0, 3.0)),
                BusStop("4", name = "Stop 4", location = GeoPoint(4.0, 4.0)),
                BusStop("5", name = "Stop 5", location = GeoPoint(5.0, 5.0)),
                BusStop("6", name = "Stop 6", location = GeoPoint(6.0, 6.0)),
            )
        val busLines =
            listOf(
                BusLine("2-5", "Line 2", listOf(busStops[2], busStops[3], busStops[4], busStops[5])),
                BusLine("5-0", "Line 3", listOf(busStops[5], busStops[6], busStops[0])),
                BusLine("0-5", "Line 4", listOf(busStops[0], busStops[1], busStops[2], busStops[3], busStops[4], busStops[5])),
            )
        every { busRepository.allBusStops } returns busStops
        every { busRepository.allBusLines } returns busLines

        every { busRepository.getNearestStops(any(), any()) } returns busStops
        busStops.forEach { busStop ->
            every { busRepository.getLinesForStop(busStop) } returns busLines.filter { it.stops.contains(busStop) }
        }
    }

    @Test
    fun searchTest() {
        val origin = GeoPoint(1.2345, 2.3456)
        val destination = GeoPoint(3.4567, 4.5678)
        val busRide = busRoutingService.search(origin, destination)
        assertNotNull(busRide)
        assertTrue(busRide.busLine.name == "Line 2")
        assertTrue(busRide.stops.size == 3)
        assertTrue(busRide.stops.first().name == "Stop 2")
        assertTrue(busRide.stops.last().name == "Stop 4")
    }

    @Test
    fun `origin is between two stations closer to first`() {
        val origin = GeoPoint(0.5, 0.3)
        val destination = GeoPoint(4.7567, 4.5678)
        val busRide = busRoutingService.search(origin, destination)
        assertNotNull(busRide)
        assertTrue(busRide.busLine.name == "Line 4")
        assertEquals(6, busRide.stops.size)
        assertTrue(busRide.stops.first().name == "Stop 0")
        assertTrue(busRide.stops.last().name == "Stop 5")
    }

    @Test
    fun `origin is between two stations closer to second`() {
        val origin = GeoPoint(0.5, 0.8)
        val destination = GeoPoint(4.7567, 4.5678)
        val busRide = busRoutingService.search(origin, destination)
        assertNotNull(busRide)
        assertTrue(busRide.busLine.name == "Line 4")
        assertEquals(5, busRide.stops.size)
        assertTrue(busRide.stops.first().name == "Stop 1")
        assertTrue(busRide.stops.last().name == "Stop 5")
    }

    companion object {
    }
}
