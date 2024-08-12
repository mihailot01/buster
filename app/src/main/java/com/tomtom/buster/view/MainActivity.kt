package com.tomtom.buster.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentContainerView
import com.tomtom.buster.R
import com.tomtom.buster.viewmodel.MapViewModel
import com.tomtom.buster.viewmodel.NavigationViewModel
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraChangeListener
import com.tomtom.sdk.map.display.camera.CameraTrackingMode
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.gesture.MapLongClickListener
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.marker.Marker
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.currentlocation.CurrentLocationButton
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.routing.route.RouteId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val MAP_FRAGMENT_TAG = "MAP_FRAGMENT_TAG"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mapViewModel: MapViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()

    private lateinit var tomTomMap: TomTomMap

    @Inject lateinit var mapFragment: MapFragment

    @Inject lateinit var navigationFragment: NavigationFragment

    private val bottomSheet: LinearLayout by lazy { findViewById(R.id.bottom_sheet) }
    private val mapContainer: FragmentContainerView by lazy { findViewById(R.id.map_container) }
    private val cancelButton: Button by lazy { findViewById(R.id.cancel_button) }
    private val startButton: Button by lazy { findViewById(R.id.start_button) }
    private val etaTextView: TextView by lazy { findViewById(R.id.eta) }
    private val lengthTextView: TextView by lazy { findViewById(R.id.length) }
    private val busLineTextView: TextView by lazy { findViewById(R.id.bus_line) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initButtonListeners()

        mapViewModel.routeOptions.observe(this) { newValue ->
            if (newValue != null) {
                if (!navigationViewModel.isNavigationRunning()) {
                    showBottomSheet()
                }
            } else {
                hideBottomSheet()
            }
        }

        if (areLocationPermissionsGranted()) {
            mapInitiation()
            return
        }
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            if (it.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) &&
                it.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            ) {
                mapInitiation()
            } else {
                Toast.makeText(this@MainActivity, "Location must be allowed for this application", Toast.LENGTH_SHORT).show()
            }
        }.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun showBottomSheet() {
        bottomSheet.visibility = View.VISIBLE
        val eta = mapViewModel.activeRoute.value?.summary?.travelTime ?: getString(R.string.unavailable)
        val length = mapViewModel.activeRoute.value?.summary?.length ?: getString(R.string.unavailable)
        val waypoints = mapViewModel.activeRoute.value?.waypoints ?: emptyList()
        val busLine =
            waypoints.firstOrNull()?.place?.name?.substringAfter("bus: ")?.trim()
                ?: getString(R.string.unavailable)

        etaTextView.text = eta.toString()
        lengthTextView.text = length.toString()
        busLineTextView.text = busLine.toString()

        mapContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToTop = R.id.bottom_sheet
        }
    }

    private fun hideBottomSheet() {
        bottomSheet.visibility = LinearLayout.GONE
        mapContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToBottom = R.id.main_activity_layout
        }
    }

    private fun initButtonListeners() {
        cancelButton.setOnClickListener {
            mapViewModel.clearRoute()
        }
        startButton.setOnClickListener {
            hideBottomSheet()
            initNavigationFragment()
            startNavigation()
        }
    }

    private fun mapInitiation() {
        mapViewModel.routeOptions.observeForever { newValue ->
            if (newValue != null) {
                showRoute(newValue)
            } else {
                clearMap()
            }
        }

        mapViewModel.failureMessage.observeForever { newValue ->
            newValue?.let {
                Toast.makeText(this, newValue, Toast.LENGTH_SHORT).show()
            }
        }

        mapViewModel.activeRoute.observeForever { newValue ->
            newValue?.let { route ->
                route.waypoints.forEach {
                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_map_marker)
                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, BUS_STOP_ICON_WIDTH, BUS_STOP_ICON_HEIGHT, false)
                    val markerOptions =
                        MarkerOptions(
                            coordinate = it.place.coordinate,
                            pinImage = ImageFactory.fromBitmap(resizedBitmap),
                            balloonText = it.place.name.substring(0, it.place.name.indexOf("bus")).replace("stop: ", "").trim(),
                        )
                    tomTomMap.addMarker(markerOptions)
                    tomTomMap.addMarkerClickListener { marker: Marker ->
                        if (!marker.isSelected()) {
                            marker.select()
                        } else {
                            marker.deselect()
                        }
                    }
                }
            }
        }

        mapFragment.getMapAsync { map ->
            tomTomMap = map
            mapViewModel.showUserLocation(tomTomMap)
            setUpMapListeners()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment, MAP_FRAGMENT_TAG)
            .commit()
        supportFragmentManager.beginTransaction()
            .add(R.id.navigation_fragment_container, navigationFragment)
            .commit()
    }

    private fun initNavigationFragment() {
        val navigationFragmentContainer = findViewById<FragmentContainerView>(R.id.navigation_fragment_container)
        navigationFragmentContainer.visibility = View.VISIBLE
    }

    private fun showRoute(
        routeOptions: RouteOptions,
        withZoom: Boolean = true,
    ) {
        tomTomMap.addRoute(routeOptions)
        if (withZoom) {
            tomTomMap.zoomToRoutes(ZOOM_TO_ROUTE_PADDING)
        }
    }

    private val mapLongClickListener =
        MapLongClickListener { geoPoint ->
            if (navigationViewModel.isNavigationRunning()) {
                return@MapLongClickListener false
            }
            clearMap()
            val userLocation = tomTomMap.currentLocation?.position
            userLocation?.let { location ->
                mapViewModel.calculateRouteTo(location, geoPoint)
                true
            } ?: false
        }

    private fun clearMap() {
        tomTomMap.clear()
    }

    private fun setUpMapListeners() {
        tomTomMap.addMapLongClickListener(mapLongClickListener)
    }

    private fun areLocationPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun startNavigation() {
        navigationFragment.setTomTomNavigation(navigationViewModel.tomTomNavigation)
        navigationViewModel.getRoutePlan()?.let {
            navigationFragment.startNavigation(it)
            navigationFragment.addNavigationListener(navigationListener)
            navigationViewModel.setListeners()
            addNavigationListeners()
        }
    }

    private val navigationListener =
        object : NavigationFragment.NavigationListener {
            override fun onStarted() {
                tomTomMap.addCameraChangeListener(cameraChangeListener)
                tomTomMap.cameraTrackingMode = CameraTrackingMode.FollowRouteDirection
                tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Chevron))
                tomTomMap.setLocationProvider(navigationViewModel.locationService.locationProvider)
                setMapNavigationPadding()
            }

            override fun onStopped() {
                stopNavigation()
            }
        }

    private val cameraChangeListener by lazy {
        CameraChangeListener {
            val cameraTrackingMode = tomTomMap.cameraTrackingMode
            if (cameraTrackingMode == CameraTrackingMode.FollowRouteDirection) {
                navigationFragment.navigationView.showSpeedView()
            } else {
                navigationFragment.navigationView.hideSpeedView()
            }
        }
    }

    private fun setMapNavigationPadding() {
        val paddingBottom = resources.getDimensionPixelOffset(R.dimen.map_padding_bottom)
        val padding = Padding(0, 0, 0, paddingBottom)
        tomTomMap.setPadding(padding)
    }

    private fun stopNavigation() {
        navigationFragment.stopNavigation()
        mapFragment.currentLocationButton.visibilityPolicy =
            CurrentLocationButton.VisibilityPolicy.InvisibleWhenRecentered
        tomTomMap.removeCameraChangeListener(cameraChangeListener)
        tomTomMap.cameraTrackingMode = CameraTrackingMode.None
        tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Pointer))
        tomTomMap.setPadding(Padding(0, 0, 0, 0))
        navigationFragment.removeNavigationListener(navigationListener)
        navigationViewModel.removeListeners()
        removeNavigationListeners()
        clearMap()
        mapViewModel.showUserLocation(tomTomMap)
        navigationViewModel.clearRoutes()
    }

    private fun addNavigationListeners() {
        navigationViewModel.distanceAlongRoute.observe(this) {
            tomTomMap.routes.first().progress = it
            tomTomMap.routes.first()
        }
        navigationViewModel.removedRoute.observe(this) { route ->
            tomTomMap.routes.find { it.tag == route.id.toString() }?.remove()
        }
        navigationViewModel.addedRoute.observe(this) { route ->
            route?.let {
                clearMap()
                mapViewModel.setRoute(it)
            }
        }

        navigationViewModel.activeRoute.observe(this) { route ->
            route?.let {
                adjustRouteColors(it.id)
            }
        }
    }

    private fun adjustRouteColors(activeRouteId: RouteId) {
        tomTomMap.routes.forEach {
            if (it.tag == activeRouteId.toString()) {
                it.color = RouteOptions.DEFAULT_COLOR
            } else {
                it.color = Color.GRAY
            }
        }
    }

    private fun removeNavigationListeners() {
        navigationViewModel.distanceAlongRoute.removeObservers(this)
        navigationViewModel.removedRoute.removeObservers(this)
        navigationViewModel.addedRoute.removeObservers(this)
        navigationViewModel.activeRoute.removeObservers(this)
    }

    companion object {
        private const val ZOOM_TO_ROUTE_PADDING = 50
        private const val BUS_STOP_ICON_WIDTH = 80
        private const val BUS_STOP_ICON_HEIGHT = 100
    }
}
