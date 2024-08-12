package com.tomtom.buster.navsdkextensions.model

import android.util.Log
import com.tomtom.buster.navsdkextensions.service.OpenStreetMapApi
import com.tomtom.sdk.location.GeoPoint

internal class OpenStreetMapBusRepository(private val openStreetMapApi: OpenStreetMapApi) : BusRepositoryInterface {
    private var busStops: List<BusStop> = emptyList()
    private var busLines: List<BusLine> = emptyList()

    override val allBusStops: List<BusStop>
        get() = busStops
    override val allBusLines: List<BusLine>
        get() = busLines

    suspend fun loadData() {
        val stopsResult = openStreetMapApi.getAllBusStops()
        val linesResult = openStreetMapApi.getAllBusLines()
        if (stopsResult.isFailure() || linesResult.isFailure()) {
            if (stopsResult.isFailure()) {
                Log.e("OpenStreetMapBusRepository", stopsResult.failure().message.toString())
            }
            if (linesResult.isFailure()) {
                Log.e("OpenStreetMapBusRepository", stopsResult.failure().message.toString())
            }
            return
        }
        val stops = stopsResult.value()
        val lines = linesResult.value()
        val map =
            stops.associate { stop ->
                stop.nodeId to
                    BusStop(
                        stop.stopId,
                        stop.name,
                        GeoPoint(stop.lat, stop.lon),
                    )
            }
        busStops = map.toList().map { it.second }
        busLines =
            lines.map { line ->
                val stopsForLine =
                    stops.filter { stop -> line.stopIds.contains(stop.nodeId) }
                        .sortedBy { stop ->
                            line.stopIds.indexOf(stop.nodeId)
                        }.map { stop ->
                            map[stop.nodeId]!!
                        }
                BusLine(line.id, line.name, stopsForLine)
            }
    }

    override fun getLinesForStop(stop: BusStop): List<BusLine> {
        return busLines.filter { it.stops.contains(stop) }
    }

    override fun getNearestStops(
        location: GeoPoint,
        numberOfStops: Number,
    ): List<BusStop> {
        return busStops.sortedBy {
            val latDiff = it.location.latitude - location.latitude
            val lonDiff = it.location.longitude - location.longitude
            latDiff * latDiff + lonDiff * lonDiff
        }.take(numberOfStops.toInt())
    }
}
