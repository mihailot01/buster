package com.tomtom.buster.navsdkextensions

import com.squareup.moshi.Moshi
import com.tomtom.buster.navsdkextensions.model.BusStop
import com.tomtom.buster.navsdkextensions.model.OpenStreetMapBusRepository
import com.tomtom.buster.navsdkextensions.service.LineResponse
import com.tomtom.buster.navsdkextensions.service.NodeResponse
import com.tomtom.buster.navsdkextensions.service.OpenStreetMapApi
import com.tomtom.buster.navsdkextensions.service.OpenStreetMapApiService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpenStreetMapRepositoryTest {
    private val query = slot<String>()

    @Test
    fun testGetBusStops() =
        runTest {
            val retrofitClientMock = mockk<OpenStreetMapApiService>()
            coEvery { retrofitClientMock.getOpenStreetMapDataLines(capture(query)) } coAnswers {
                Moshi.Builder().build().adapter(LineResponse::class.java).fromJson(File("src/test/res/LinesResponse.json").readText())!!
            }

            coEvery { retrofitClientMock.getOpenStreetMapDataNodes(capture(query)) } coAnswers {
                Moshi.Builder().build().adapter(NodeResponse::class.java).fromJson(File("src/test/res/StopsResponse.json").readText())!!
            }

            val openStreetMapApiMock = OpenStreetMapApi(retrofitClientMock)
            val openStreetMapBusRepository = OpenStreetMapBusRepository(openStreetMapApiMock)
            openStreetMapBusRepository.loadData()

            assertEquals(3215, openStreetMapBusRepository.allBusStops.size)
            assertEquals(357, openStreetMapBusRepository.allBusLines.size)
            val stop1172: BusStop? = openStreetMapBusRepository.allBusStops.find { it.id == "1172" }
            val stop412: BusStop? = openStreetMapBusRepository.allBusStops.find { it.id == "412" }
            val stop1171: BusStop? = openStreetMapBusRepository.allBusStops.find { it.id == "1171" }
            val stop410: BusStop? = openStreetMapBusRepository.allBusStops.find { it.id == "410" }
            assertNotNull(stop1172)
            assertNotNull(stop412)
            assertEquals("Maksima Brankovića", stop1172!!.name)
            assertTrue(openStreetMapBusRepository.allBusLines.count { it.name.contains("706") } == 4)
            val line706BZ = openStreetMapBusRepository.allBusLines.first { it.id == "2810357" }
            val line706ZB = openStreetMapBusRepository.allBusLines.first { it.id == "2810356" }
            val stops = line706BZ.stops
            println(line706BZ)
            assertTrue(stops.contains(stop1172))
            assertTrue(stops.contains(stop412))
            assertTrue(stops.indexOf(stop1172) < stops.indexOf(stop412))
            val stops2 = line706ZB.stops
            assertTrue(stops2.contains(stop1171))
            assertTrue(stops2.contains(stop410))
            assertTrue(stops2.indexOf(stop1171) > stops2.indexOf(stop410))
        }
}
