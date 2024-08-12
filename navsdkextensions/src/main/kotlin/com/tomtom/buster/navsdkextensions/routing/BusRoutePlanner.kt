package com.tomtom.buster.navsdkextensions.routing

import android.content.Context
import com.tomtom.buster.navsdkextensions.model.OpenStreetMapBusRepository
import com.tomtom.buster.navsdkextensions.service.BusSearchService
import com.tomtom.buster.navsdkextensions.service.OpenStreetMapApi
import com.tomtom.sdk.common.Cancellable
import com.tomtom.sdk.common.Result
import com.tomtom.sdk.location.Place
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.ItineraryPoint
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.guidance.RouteIncrementOptions
import com.tomtom.sdk.routing.route.Route
import kotlinx.coroutines.runBlocking

/**
 * Enables the planning of a valid bus route that can be integrated into TomTom navigation.
 * This class serves as an entry point for executing route planning actions based on TomTom's Routing APIs.
 */
class BusRoutePlanner internal constructor(
    private val routePlanner: RoutePlanner,
    private val busSearchService: BusSearchService,
) : RoutePlanner {
    @Throws(IllegalArgumentException::class)
    private fun provideBusItinerary(itinerary: Itinerary): Itinerary {
        require(itinerary.waypoints.isEmpty()) { WAYPOINTS_UNSUPPORTED }
        val busRide = busSearchService.search(itinerary.origin.place.coordinate, itinerary.destination.place.coordinate)
        if (busRide.stops.size < 2) {
            return itinerary
        } else {
            return Itinerary(
                origin = itinerary.origin,
                destination = itinerary.destination,
                waypoints =
                    busRide.stops.map {
                        ItineraryPoint(
                            Place(coordinate = it.location, name = "stop: ${it.name}\nbus: ${busRide.busLine.name}"),
                        )
                    },
            )
        }
    }

    override fun advanceGuidanceProgress(routeIncrementOptions: RouteIncrementOptions): Result<Route, RoutingFailure> {
        return routePlanner.advanceGuidanceProgress(routeIncrementOptions)
    }

    override fun close() {
        routePlanner.close()
    }

    override fun planRoute(routePlanningOptions: RoutePlanningOptions): Result<RoutePlanningResponse, RoutingFailure> {
        val newRoutePlanningOptions =
            routePlanningOptions.copy(
                itinerary = provideBusItinerary(routePlanningOptions.itinerary),
            )
        return routePlanner.planRoute(newRoutePlanningOptions)
    }

    override fun planRoute(
        routePlanningOptions: RoutePlanningOptions,
        callback: RoutePlanningCallback,
    ): Cancellable {
        val newRoutePlanningOptions =
            routePlanningOptions.copy(
                itinerary = provideBusItinerary(routePlanningOptions.itinerary),
            )
        return routePlanner.planRoute(newRoutePlanningOptions, callback)
    }

    companion object {
        /**
         * Creates a new instance of [BusRoutePlanner].
         * Only one instance of [BusRoutePlanner] should be created per application.
         * @param context The context of the application.
         * @param apiKey The API key to be used for routing.
         */

        val WAYPOINTS_UNSUPPORTED = "BusRoutePlanner does not support waypoints"

        fun create(
            context: Context,
            apiKey: String,
        ): BusRoutePlanner {
            val openStreetMapBusRepository = OpenStreetMapBusRepository(OpenStreetMapApi.create())
            runBlocking {
                openStreetMapBusRepository.loadData()
            }
            return BusRoutePlanner(OnlineRoutePlanner.create(context, apiKey), BusSearchService(openStreetMapBusRepository))
        }
    }
}
