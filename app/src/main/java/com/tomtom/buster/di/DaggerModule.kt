package com.tomtom.buster.di

import android.content.Context
import com.tomtom.buster.BuildConfig
import com.tomtom.buster.model.RouteRepository
import com.tomtom.buster.navsdkextensions.engine.BusGuidanceEngine
import com.tomtom.buster.navsdkextensions.engine.BusRouteTrackingEngine
import com.tomtom.buster.navsdkextensions.navigation.BusArrivalDetectionEngine
import com.tomtom.buster.navsdkextensions.navigation.BusRouteReplanner
import com.tomtom.buster.navsdkextensions.routing.BusRoutePlanner
import com.tomtom.quantity.Distance
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStore
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStoreConfiguration
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.location.android.AndroidLocationProviderConfig
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.navigation.online.Configuration
import com.tomtom.sdk.navigation.online.OnlineTomTomNavigationFactory
import com.tomtom.sdk.navigation.replanning.RouteReplanningEngineFactory
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class TomTomApiKey

@Module
@InstallIn(SingletonComponent::class)
object DaggerModule {
    @Provides
    @Singleton
    @TomTomApiKey
    fun provideApiKey(): String {
        return BuildConfig.TOMTOM_API_KEY
    }

    @Provides
    @Singleton
    fun provideMapOptions(
        @TomTomApiKey apiKey: String,
    ): MapOptions {
        return MapOptions(mapKey = apiKey)
    }

    @Provides
    @Singleton
    fun provideMapFragment(mapOptions: MapOptions): MapFragment {
        return MapFragment.newInstance(mapOptions)
    }

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context,
    ): LocationProvider {
        return AndroidLocationProvider(
            context,
            AndroidLocationProviderConfig(
                minTimeInterval = 2.seconds,
                minDistance = Distance.meters(2),
            ),
        )
    }

    @Provides
    @Singleton
    fun provideRouteRepository(): RouteRepository {
        return RouteRepository()
    }

    @Provides
    @Singleton
    fun provideBusRoutePlanner(
        @ApplicationContext context: Context,
        @TomTomApiKey apiKey: String,
    ): BusRoutePlanner {
        return BusRoutePlanner.create(context = context, apiKey = apiKey)
    }

    @Provides
    @Singleton
    fun provideRoutePlanner(
        @ApplicationContext context: Context,
        @TomTomApiKey apiKey: String,
    ): RoutePlanner {
        return OnlineRoutePlanner.create(context, apiKey)
    }

    @Provides
    @Singleton
    fun provideBusRouteReplanner(
        busRoutePlanner: BusRoutePlanner,
        routePlanner: RoutePlanner,
    ): BusRouteReplanner {
        return BusRouteReplanner.create(busRoutePlanner = busRoutePlanner, routePlanner = routePlanner)
    }

    @Provides
    @Singleton
    fun provideBusArrivalDetectionEngine(): BusArrivalDetectionEngine {
        return BusArrivalDetectionEngine.create()
    }

    @Provides
    @Singleton
    fun provideNavigationTileStore(
        @ApplicationContext context: Context,
        @TomTomApiKey apiKey: String,
    ): NavigationTileStore {
        return NavigationTileStore.create(
            context = context,
            navigationTileStoreConfig = NavigationTileStoreConfiguration(apiKey),
        )
    }

    @Provides
    fun provideTomTomNavigation(
        @ApplicationContext context: Context,
        navigationTileStore: NavigationTileStore,
        locationProvider: LocationProvider,
        busRoutePlanner: BusRoutePlanner,
        busArrivalDetectionEngine: BusArrivalDetectionEngine,
        busRouteReplanner: BusRouteReplanner,
    ): TomTomNavigation {
        val tomTomNavigation =
            OnlineTomTomNavigationFactory.create(
                Configuration(
                    context = context,
                    navigationTileStore = navigationTileStore,
                    locationProvider = locationProvider,
                    routePlanner = busRoutePlanner,
                    routeTrackingEngine = BusRouteTrackingEngine.create(),
                    routeReplanningEngine = RouteReplanningEngineFactory.create(busRouteReplanner),
                    guidanceEngine = BusGuidanceEngine.create(context),
                    arrivalDetectionEngine = busArrivalDetectionEngine,
                ),
            )
        tomTomNavigation.preferredLanguage = Locale.US
        return tomTomNavigation
    }

    @Provides
    @Singleton
    fun provideNavigationFragment(): NavigationFragment {
        val navigationUiOptions =
            NavigationUiOptions(
                keepInBackground = true,
                showWaypointArrivalPanel = true,
            )
        return NavigationFragment.newInstance(navigationUiOptions)
    }
}
