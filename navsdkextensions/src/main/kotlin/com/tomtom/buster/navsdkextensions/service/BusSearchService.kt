package com.tomtom.buster.navsdkextensions.service

import com.tomtom.buster.navsdkextensions.model.BusRepositoryInterface
import com.tomtom.buster.navsdkextensions.model.BusRide
import com.tomtom.sdk.location.GeoPoint

class BusSearchService(
    private val busRepository: BusRepositoryInterface,
) : BusSearchServiceInterface {
    private val numberOfStops: Number = 10

    override fun search(
        origin: GeoPoint,
        destination: GeoPoint,
    ): BusRide {
        val originStops = busRepository.getNearestStops(origin, numberOfStops)
        val lines = originStops.map { busRepository.getLinesForStop(it) }
        val busRides =
            originStops.zip(lines).map { (originStop, lines) ->
                val line =
                    lines.minBy { line ->
                        val originStopIndex = line.stops.indexOf(originStop)
                        val stops = line.stops.subList(originStopIndex, line.stops.size)
                        stops.minOf { stop -> stop.location.distanceTo(destination) }
                    }
                val originStopIndex = line.stops.indexOf(originStop)
                val stops = line.stops.subList(originStopIndex, line.stops.size)
                val destinationStop = stops.minBy { stop -> stop.location.distanceTo(destination) }
                BusRide(line, stops.subList(0, stops.indexOf(destinationStop) + 1))
            }
        return busRides.minBy { busRide ->
            busRide.stops.last().location.distanceTo(destination) + busRide.stops.first().location.distanceTo(origin)
        }
    }
}
