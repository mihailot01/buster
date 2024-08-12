package com.tomtom.buster.navsdkextensions

import com.tomtom.buster.navsdkextensions.model.BusLine
import com.tomtom.buster.navsdkextensions.model.BusRepositoryInterface
import com.tomtom.buster.navsdkextensions.model.BusStop
import com.tomtom.sdk.location.GeoPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockBusRepository : BusRepositoryInterface {
    private val busStops = mutableListOf<BusStop>()
    private val busLines = mutableListOf<BusLine>()

    init {
        loadData()
    }

    override val allBusStops = busStops

    override val allBusLines = busLines

    private fun loadData() {
        busStops.addAll(
            listOf(
                BusStop("0", "Stop 0", GeoPoint(0.0, 0.0)),
                BusStop("1", "Stop 1", GeoPoint(1.0, 1.0)),
                BusStop("2", "Stop 2", GeoPoint(2.0, 2.0)),
                BusStop("3", "Stop 3", GeoPoint(3.0, 3.0)),
                BusStop("4", "Stop 4", GeoPoint(4.0, 4.0)),
                BusStop("5", "Stop 5", GeoPoint(5.0, 5.0)),
            ),
        )
        busLines.addAll(
            listOf(
                BusLine("0-2", "Line 1", listOf(busStops[0], busStops[1], busStops[2])),
                BusLine("2-5", "Line 2", listOf(busStops[2], busStops[3], busStops[4], busStops[5])),
                BusLine("5-0", "Line 3", listOf(busStops[5], busStops[0])),
                BusLine("0-5", "Line 4", listOf(busStops[0], busStops[1], busStops[2], busStops[3], busStops[4], busStops[5])),
            ),
        )
    }

    override fun getLinesForStop(stop: BusStop): List<BusLine> {
        return allBusLines.filter { it.stops.contains(stop) }
    }

    override fun getNearestStops(
        location: GeoPoint,
        numberOfStops: Number,
    ): List<BusStop> {
        return allBusStops.sortedBy {
            val latDiff = it.location.latitude - location.latitude
            val lonDiff = it.location.longitude - location.longitude
            latDiff * latDiff + lonDiff * lonDiff
        }.take(numberOfStops.toInt())
    }
}

class BusRepositoryTest {
    private var busRepository: BusRepositoryInterface = MockBusRepository()

    @Test
    fun testGetLinesForStop() {
        val busLines = busRepository.allBusLines
        val busStop = busRepository.allBusStops[0]

        val linesForStop = busRepository.getLinesForStop(busStop)

        assertTrue(linesForStop.contains(busLines[0]))
        assertTrue(linesForStop.contains(busLines[2]))
        assertFalse(linesForStop.contains(busLines[1]))
    }

    @Test
    fun testGetNearestStops() {
        val busStops = busRepository.allBusStops

        val nearestStops = busRepository.getNearestStops(GeoPoint(0.0, 0.0), 5)

        assertTrue(nearestStops.contains(busStops[0]))
        assertFalse(nearestStops.contains(busStops[5]))
    }
}
