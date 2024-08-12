package com.tomtom.buster.navsdkextensions

import com.tomtom.buster.navsdkextensions.navigation.BusArrivalDetectionEngine
import com.tomtom.sdk.navigation.arrival.ArrivalDetectionEngine
import com.tomtom.sdk.navigation.arrival.DefaultArrivalDetectionEngineFactory
import com.tomtom.sdk.navigation.arrival.DestinationArrivalStatus
import com.tomtom.sdk.navigation.arrival.WaypointStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BusArrivalDetectionEngineTest {
    private lateinit var busArrivalDetectionEngine: BusArrivalDetectionEngine
    private lateinit var arrivalDetectionEngine: ArrivalDetectionEngine
    @Before
    fun setup() {
        arrivalDetectionEngine = mockk()
        mockkStatic(DefaultArrivalDetectionEngineFactory::class)
        every { DefaultArrivalDetectionEngineFactory.create(any()) } returns arrivalDetectionEngine
        busArrivalDetectionEngine = BusArrivalDetectionEngine.create(mockk())
    }
    @Test
    fun `has arrived at destination behaves same as DefaultArrivalDetectionEngine has arrived at destination`() {
        val destinationArrivalStatus = mockk<DestinationArrivalStatus>()
        every { arrivalDetectionEngine.hasArrivedAtDestination(any()) } returns destinationArrivalStatus
        assertEquals(destinationArrivalStatus, busArrivalDetectionEngine.hasArrivedAtDestination(mockk()))
    }

    @Test
    fun `has arrived at waypoint behaves same as DefaultArrivalDetectionEngine has arrived at waypoint`() {
        val waypointStatus = mockk<WaypointStatus>()
        every { arrivalDetectionEngine.hasArrivedAtWaypoint(any()) } returns waypointStatus
        assertEquals(waypointStatus, busArrivalDetectionEngine.hasArrivedAtWaypoint(mockk()))
    }

}