package com.tomtom.buster.navsdkextensions.engine

import com.tomtom.buster.navsdkextensions.RouteLegDistinctor
import com.tomtom.sdk.navigation.NavigationSnapshot
import com.tomtom.sdk.navigation.tracking.RouteTrackingEngine
import com.tomtom.sdk.navigation.tracking.RouteTrackingEngineFactory
import com.tomtom.sdk.navigation.tracking.RouteTrackingState

class BusRouteTrackingEngine internal constructor(private val defaultRouteTrackingEngine: RouteTrackingEngine) : RouteTrackingEngine {
    override fun close() {
        defaultRouteTrackingEngine.close()
    }

    override fun obtainTrackingStates(navigationSnapshot: NavigationSnapshot): RouteTrackingState {
        val routeTrackingState = defaultRouteTrackingEngine.obtainTrackingStates(navigationSnapshot)
        val activeRoute = navigationSnapshot.tripSnapshot!!.activeRoute.routePlan.route
        val followedRoutes = routeTrackingState.followedRoutes
        return if (RouteLegDistinctor.checkIfOnBusLeg(navigationSnapshot.tripSnapshot!!.activeRoute)) {
            RouteTrackingState(
                if (activeRoute in followedRoutes) followedRoutes else followedRoutes + activeRoute,
                routeTrackingState.unfollowedRoutes.filter { it.route != activeRoute },
            )
        } else {
            routeTrackingState
        }
    }

    companion object {
        fun create() = BusRouteTrackingEngine(RouteTrackingEngineFactory.create())
    }
}
