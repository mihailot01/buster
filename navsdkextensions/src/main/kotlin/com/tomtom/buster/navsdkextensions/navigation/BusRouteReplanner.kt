package com.tomtom.buster.navsdkextensions.navigation

import com.tomtom.sdk.annotations.InternalTomTomSdkApi
import com.tomtom.sdk.common.Result
import com.tomtom.sdk.navigation.NavigationSnapshot
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.copy
import com.tomtom.sdk.navigation.replanning.RouteReplanningFailure
import com.tomtom.sdk.navigation.routereplanner.RouteReplanner
import com.tomtom.sdk.navigation.routereplanner.RouteReplannerResponse
import com.tomtom.sdk.navigation.routereplanner.online.OnlineRouteReplannerFactory
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.options.Itinerary

/**
 * Enables the planning of a valid bus route during navigation that can be integrated into TomTom navigation.
 * This class serves as an entry point for route planning that can be dynamically updated.
 */
class BusRouteReplanner internal constructor(
    private val routeReplannerUsingBusRoutePlanner: RouteReplanner,
    private val routeReplannerUsingOnlineRoutePlanner: RouteReplanner,
) : RouteReplanner {
    @OptIn(InternalTomTomSdkApi::class)
    override fun backToRoute(navigationSnapshot: NavigationSnapshot): Result<RouteReplannerResponse, RouteReplanningFailure> {
        // TODO(BUSTER-49): Remove this when new version of NavSDK is released
        val newNavigationSnapshot =
            navigationSnapshot.copy(
                tripSnapshot =
                    navigationSnapshot.tripSnapshot!!.copy(
                        hasDeviated = true,
                    ),
            )
        return routeReplannerUsingBusRoutePlanner.backToRoute(newNavigationSnapshot)
    }

    override fun incrementRouteContents(navigationSnapshot: NavigationSnapshot): Result<RouteReplannerResponse, RouteReplanningFailure> {
        return routeReplannerUsingOnlineRoutePlanner.incrementRouteContents(navigationSnapshot)
    }

    @OptIn(InternalTomTomSdkApi::class)
    override fun update(navigationSnapshot: NavigationSnapshot): Result<RouteReplannerResponse, RouteReplanningFailure> {
        val tripSnapshot = navigationSnapshot.tripSnapshot!!
        val routePlan = tripSnapshot.activeRoute.routePlan
        val routePlanningOptions =
            routePlan.routePlanningOptions.copy(
                itinerary =
                    Itinerary(
                        origin = navigationSnapshot.locationSnapshot.rawLocation.position,
                        destination = routePlan.route.destination.place.coordinate,
                        waypoints = routePlan.route.waypoints.map { it.place.coordinate },
                    ),
            )
        val newRoutePlan =
            RoutePlan(
                route = routePlan.route,
                routePlanningOptions = routePlanningOptions,
            )
        val newNavigationSnapshot =
            navigationSnapshot.copy(
                tripSnapshot =
                    tripSnapshot.copy(
                        activeRoute =
                            tripSnapshot.activeRoute.copy(
                                routePlan = newRoutePlan,
                            ),
                    ),
            )
        return routeReplannerUsingOnlineRoutePlanner.update(newNavigationSnapshot)
    }

    override fun close() {
        routeReplannerUsingOnlineRoutePlanner.close()
        routeReplannerUsingBusRoutePlanner.close()
    }

    companion object {
        /**
         * Creates a new instance of [BusRouteReplanner].
         * Only one instance of [BusRouteReplanner] should be created per application.
         */
        fun create(
            busRoutePlanner: RoutePlanner,
            routePlanner: RoutePlanner,
        ) = BusRouteReplanner(
            OnlineRouteReplannerFactory.create(busRoutePlanner),
            OnlineRouteReplannerFactory.create(routePlanner),
        )
    }
}
