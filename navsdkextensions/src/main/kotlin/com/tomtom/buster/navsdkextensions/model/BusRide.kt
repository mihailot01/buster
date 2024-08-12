package com.tomtom.buster.navsdkextensions.model

data class BusRide(val busLine: BusLine, val stops: List<BusStop> = emptyList())
