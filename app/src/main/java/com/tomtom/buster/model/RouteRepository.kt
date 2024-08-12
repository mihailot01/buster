package com.tomtom.buster.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tomtom.sdk.routing.route.Route

class RouteRepository {
    private val routeMutable: MutableLiveData<Route?> = MutableLiveData()
    val route: LiveData<Route?> = routeMutable
    private val failureMessageMutable: MutableLiveData<String?> = MutableLiveData()
    val failureMessage: LiveData<String?> = failureMessageMutable

    fun clearRoute() {
        routeMutable.value = null
    }

    fun setRoute(route: Route) {
        failureMessageMutable.value = null
        routeMutable.value = route
    }

    fun setFailureMessage(failureMessage: String) {
        clearRoute()
        failureMessageMutable.value = failureMessage
    }
}
