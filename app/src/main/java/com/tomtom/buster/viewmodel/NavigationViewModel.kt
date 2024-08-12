package com.tomtom.buster.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tomtom.buster.service.LocationService
import com.tomtom.buster.service.RouteService
import com.tomtom.quantity.Distance
import com.tomtom.sdk.navigation.ActiveRouteChangedListener
import com.tomtom.sdk.navigation.NavigationState
import com.tomtom.sdk.navigation.ProgressUpdatedListener
import com.tomtom.sdk.navigation.RouteAddedListener
import com.tomtom.sdk.navigation.RouteAddedReason
import com.tomtom.sdk.navigation.RouteRemovedListener
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.routing.route.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel
    @Inject
    constructor(
        val tomTomNavigation: TomTomNavigation,
        private val routeService: RouteService,
        val locationService: LocationService,
    ) : ViewModel() {
        private val distanceAlongRouteMutable: MutableLiveData<Distance> = MutableLiveData()
        val distanceAlongRoute: LiveData<Distance> = distanceAlongRouteMutable
        private val removedRouteMutable: MutableLiveData<Route> = MutableLiveData()
        val removedRoute: LiveData<Route> = removedRouteMutable
        private val addedRouteMutable: MutableLiveData<Route?> = MutableLiveData()
        val addedRoute: LiveData<Route?> = addedRouteMutable
        private val activeRouteMutable: MutableLiveData<Route?> = MutableLiveData()
        val activeRoute: LiveData<Route?> = activeRouteMutable

        fun isNavigationRunning(): Boolean = tomTomNavigation.navigationState != NavigationState.Idle

        fun setListeners() {
            tomTomNavigation.addProgressUpdatedListener(progressUpdatedListener)
            tomTomNavigation.addRouteAddedListener(routeAddedListener)
            tomTomNavigation.addRouteRemovedListener(routeRemovedListener)
            tomTomNavigation.addActiveRouteChangedListener(activeRouteChangedListener)
        }

        private val progressUpdatedListener =
            ProgressUpdatedListener { progress ->
                distanceAlongRouteMutable.value = progress.distanceAlongRoute
            }

        private val routeAddedListener =
            RouteAddedListener { route, _, routeAddedReason ->
                if (routeAddedReason !is RouteAddedReason.NavigationStarted) {
                    addedRouteMutable.value = route
                }
            }

        private val routeRemovedListener =
            RouteRemovedListener { route, _ ->
                removedRouteMutable.value = route
            }

        private val activeRouteChangedListener =
            ActiveRouteChangedListener { route ->
                activeRouteMutable.value = route
            }

        fun removeListeners() {
            tomTomNavigation.removeProgressUpdatedListener(progressUpdatedListener)
            tomTomNavigation.removeRouteAddedListener(routeAddedListener)
            tomTomNavigation.removeRouteRemovedListener(routeRemovedListener)
            tomTomNavigation.removeActiveRouteChangedListener(activeRouteChangedListener)
        }

        fun clearRoutes() {
            addedRouteMutable.value = null
            activeRouteMutable.value = null
        }

        fun getRoutePlan() = routeService.getRoutePlan()
    }
