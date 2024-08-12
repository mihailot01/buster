package com.tomtom.buster.navsdkextensions

import com.tomtom.sdk.navigation.RouteSnapshot

object RouteLegDistinctor {
    fun checkIfOnBusLeg(activeRoute: RouteSnapshot): Boolean {
        val numberOfWaypoints = activeRoute.routePlan.route.waypoints.size
        val numberOfVisitedWaypoints = activeRoute.numberOfVisitedWaypoints
        return numberOfVisitedWaypoints in 1..<numberOfWaypoints
    }
}
