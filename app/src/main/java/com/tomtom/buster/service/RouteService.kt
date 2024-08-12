package com.tomtom.buster.service

import android.util.Log
import androidx.lifecycle.ViewModel
import com.tomtom.buster.model.RouteRepository
import com.tomtom.buster.navsdkextensions.routing.BusRoutePlanner
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.route.Instruction
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.guidance.GuidanceOptions
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.vehicle.Vehicle
import javax.inject.Inject

class RouteService
    @Inject
    constructor(
        val routeRepository: RouteRepository,
        private val busRoutePlanner: BusRoutePlanner,
    ) : ViewModel() {
        private val vehicle: Vehicle = Vehicle.Car()

        fun provideRouteOptions(route: Route?): RouteOptions? {
            return route?.let {
                val instructions =
                    route.legs
                        .flatMap { routeLeg -> routeLeg.instructions }
                        .map {
                            Instruction(
                                routeOffset = it.routeOffset,
                            )
                        }
                RouteOptions(
                    geometry = route.geometry,
                    destinationMarkerVisible = true,
                    departureMarkerVisible = true,
                    instructions = instructions,
                    routeOffset = route.routePoints.map { it.routeOffset },
                    color = RouteOptions.DEFAULT_COLOR,
                    tag = route.id.toString(),
                )
            }
        }

        fun calculateRouteTo(
            origin: GeoPoint,
            destination: GeoPoint,
        ) {
            val itinerary = Itinerary(origin = origin, destination = destination)
            val routePlanningOptions =
                RoutePlanningOptions(
                    itinerary = itinerary,
                    guidanceOptions = GuidanceOptions(),
                    vehicle = vehicle,
                )

            busRoutePlanner.planRoute(
                routePlanningOptions,
                object : RoutePlanningCallback {
                    override fun onSuccess(result: RoutePlanningResponse) {
                        routeRepository.setRoute(result.routes.first())
                    }

                    override fun onFailure(failure: RoutingFailure) {
                        Log.e("busRoutePlanner.planRoute", "Route planning failed: ${failure.message}")
                        routeRepository.setFailureMessage(failure.message)
                    }

                    override fun onRoutePlanned(route: Route) = Unit
                },
            )
        }

        fun clearRoute() {
            routeRepository.clearRoute()
        }

        fun getRoutePlan(): RoutePlan? {
            return routeRepository.route.value?.let { route ->

                val routePlanningOptions =
                    RoutePlanningOptions(
                        itinerary =
                            Itinerary(
                                origin = route.geometry.first(),
                                destination = route.geometry.last(),
                            ),
                        guidanceOptions = GuidanceOptions(),
                        vehicle = vehicle,
                    )
                RoutePlan(route, routePlanningOptions)
            }
        }
    }
