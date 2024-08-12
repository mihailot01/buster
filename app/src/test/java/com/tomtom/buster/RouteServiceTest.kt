package com.tomtom.buster

import com.tomtom.buster.model.RouteRepository
import com.tomtom.buster.navsdkextensions.routing.BusRoutePlanner
import com.tomtom.buster.service.RouteService
import com.tomtom.sdk.location.GeoPoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RouteServiceTest {
    private val routeRepositoryMock: RouteRepository = mockk<RouteRepository>()
    private val busRoutePlannerMock: BusRoutePlanner = mockk<BusRoutePlanner>()

    private val routeServiceTested: RouteService = RouteService(routeRepositoryMock, busRoutePlannerMock)

    @Before
    fun setup() {
        every { routeRepositoryMock.clearRoute() } returns Unit
        every { routeRepositoryMock.setRoute(any()) } returns Unit
        every { routeRepositoryMock.setFailureMessage(any()) } returns Unit
        every { busRoutePlannerMock.planRoute(any(), any()) } returns mockk()
    }

    @Test
    fun showUserLocationTest() {
        val userLocation = GeoPoint(40.0, 40.0)
        val destination = GeoPoint(50.0, 50.0)

        routeServiceTested.calculateRouteTo(userLocation, destination)

        verify { busRoutePlannerMock.planRoute(any(), any()) }
    }

    @Test
    fun provideRouteOptionsTest() {
        val routeOptions = routeServiceTested.provideRouteOptions(null)
        assertNull(routeOptions)
    }
}
