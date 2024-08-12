package com.tomtom.buster

import com.tomtom.buster.service.LocationService
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.map.display.TomTomMap
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class LocationServiceTest {
    private lateinit var tomTomMapMock: TomTomMap
    private lateinit var locationProviderMock: LocationProvider

    private lateinit var locationServiceTested: LocationService

    @Before
    fun setup() {
        tomTomMapMock = mockk<TomTomMap>()

        every { tomTomMapMock.setLocationProvider(any()) } returns Unit
        every { tomTomMapMock.enableLocationMarker(any()) } returns Unit

        locationProviderMock = mockk<LocationProvider>()

        every { locationProviderMock.enable() } returns Unit
        every { locationProviderMock.addOnLocationUpdateListener(any()) } returns Unit

        locationServiceTested = LocationService(locationProviderMock)
    }

    @Test
    fun showUserLocationTest() {
        locationServiceTested.showUserLocation(tomTomMapMock)

        verify { locationProviderMock.enable() }
        verify { locationProviderMock.addOnLocationUpdateListener(any()) }
        verify(exactly = 0) { locationProviderMock.removeOnLocationUpdateListener(any()) }
        verify { tomTomMapMock.setLocationProvider(any()) }
        verify { tomTomMapMock.enableLocationMarker(withArg { assertNotNull(it) }) }
    }
}
