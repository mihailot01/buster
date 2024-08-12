package com.tomtom.buster.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tomtom.buster.service.LocationService
import com.tomtom.buster.service.RouteService
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.routing.route.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val locationService: LocationService,
        private val routeService: RouteService,
    ) : ViewModel() {
        private val routeOptionsMutable: MutableLiveData<RouteOptions> = MutableLiveData()
        val routeOptions: LiveData<RouteOptions> = routeOptionsMutable
        private val failureMessageMutable: MutableLiveData<String?> = MutableLiveData()
        val failureMessage: LiveData<String?> = failureMessageMutable
        private val activeRouteMutable: MutableLiveData<Route> = MutableLiveData()
        val activeRoute: LiveData<Route> = activeRouteMutable

        init {
            routeService.routeRepository.route.observeForever { newValue ->
                newValue.let { activeRouteMutable.value = it }
                routeOptionsMutable.value = routeService.provideRouteOptions(newValue)
            }
            routeService.routeRepository.failureMessage.observeForever { newValue ->
                failureMessageMutable.value = newValue
            }
        }

        fun calculateRouteTo(
            userLocation: GeoPoint,
            destination: GeoPoint,
        ) {
            routeService.calculateRouteTo(userLocation, destination)
        }

        fun clearRoute() {
            routeService.clearRoute()
        }

        fun showUserLocation(tomTomMap: TomTomMap) {
            locationService.showUserLocation(tomTomMap)
        }

        fun setRoute(route: Route) {
            routeService.routeRepository.setRoute(route)
        }
    }
