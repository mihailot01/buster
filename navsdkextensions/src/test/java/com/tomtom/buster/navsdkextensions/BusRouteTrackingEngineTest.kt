package com.tomtom.buster.navsdkextensions

import com.tomtom.buster.navsdkextensions.engine.BusRouteTrackingEngine
import com.tomtom.sdk.navigation.NavigationSnapshot
import com.tomtom.sdk.navigation.tracking.RouteTrackingEngine
import com.tomtom.sdk.navigation.tracking.RouteTrackingState
import com.tomtom.sdk.navigation.tracking.UnfollowedRoute
import com.tomtom.sdk.routing.route.Route
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BusRouteTrackingEngineTest {
    private val navigationSnapshot = mockk<NavigationSnapshot>(relaxed = true)
    private val defaultRouteTrackingEngine = mockk<RouteTrackingEngine>(relaxed = true)
    private val activeRoute = mockk<Route>()
    private val activeRouteUnfollowed = UnfollowedRoute(activeRoute, mockk())

    @Before
    fun setup() {
        every { navigationSnapshot.tripSnapshot?.activeRoute?.routePlan?.route } returns activeRoute
        mockkObject(RouteLegDistinctor)
    }

    @Test
    fun `should call default tracking if not on bus leg (active route is followed)`() {
        val defaultRouteTrackingState = RouteTrackingState(listOf(activeRoute), emptyList())
        every { defaultRouteTrackingEngine.obtainTrackingStates(any()) } returns defaultRouteTrackingState

        every { RouteLegDistinctor.checkIfOnBusLeg(any()) } returns false

        val busRouteTrackingEngine = BusRouteTrackingEngine(defaultRouteTrackingEngine)
        assertEquals(defaultRouteTrackingState, busRouteTrackingEngine.obtainTrackingStates(navigationSnapshot))
    }

    @Test
    fun `should call default tracking if not on bus leg (active route is not followed)`() {
        val defaultRouteTrackingState = RouteTrackingState(emptyList(), listOf(activeRouteUnfollowed))
        every { defaultRouteTrackingEngine.obtainTrackingStates(any()) } returns defaultRouteTrackingState

        every { RouteLegDistinctor.checkIfOnBusLeg(any()) } returns false

        val busRouteTrackingEngine = BusRouteTrackingEngine(defaultRouteTrackingEngine)
        assertEquals(defaultRouteTrackingState, busRouteTrackingEngine.obtainTrackingStates(navigationSnapshot))
    }

    @Test
    fun `should add active route to followed routes if on bus leg`() {
        val defaultRouteTrackingState = RouteTrackingState(emptyList(), listOf(activeRouteUnfollowed))
        every { defaultRouteTrackingEngine.obtainTrackingStates(any()) } returns defaultRouteTrackingState
        every { RouteLegDistinctor.checkIfOnBusLeg(any()) } returns true

        val busRouteTrackingEngine = BusRouteTrackingEngine(defaultRouteTrackingEngine)
        val routeTrackingState = busRouteTrackingEngine.obtainTrackingStates(navigationSnapshot)
        assertEquals(1, routeTrackingState.followedRoutes.size)
        assertTrue(routeTrackingState.followedRoutes.first() == activeRoute)
        assertTrue(routeTrackingState.unfollowedRoutes.isEmpty())
    }
}
