package com.tomtom.buster.service

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.OnLocationUpdateListener
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import javax.inject.Inject

@SuppressLint("MissingPermission")
class LocationService
    @Inject
    constructor(
        val locationProvider: LocationProvider,
    ) : ViewModel() {
        private lateinit var onLocationUpdateListener: OnLocationUpdateListener

        fun showUserLocation(map: TomTomMap) {
            locationProvider.enable()

            onLocationUpdateListener =
                OnLocationUpdateListener { location ->
                    map.moveCamera(CameraOptions(location.position, zoom = zoomLevelConst))
                    locationProvider.removeOnLocationUpdateListener(onLocationUpdateListener)
                }
            locationProvider.addOnLocationUpdateListener(onLocationUpdateListener)
            map.setLocationProvider(locationProvider)
            val locationMarker = LocationMarkerOptions(type = LocationMarkerOptions.Type.Pointer)
            map.enableLocationMarker(locationMarker)
        }

        companion object {
            private val zoomLevelConst = 14.0
        }
    }
