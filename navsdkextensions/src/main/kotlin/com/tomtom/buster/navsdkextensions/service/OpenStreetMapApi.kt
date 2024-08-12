package com.tomtom.buster.navsdkextensions.service

import com.squareup.moshi.Json
import com.tomtom.sdk.common.Result
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.lang.Error

internal data class BusLineResponse(
    val id: String,
    val name: String,
    val stopIds: List<String>,
)

internal data class BusStopResponse(
    val nodeId: String,
    val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)

internal data class LineResponse(
    @field:Json(name = "elements") val lines: List<Line>,
    @field:Json(name = "tags") val tags: Tags,
) {
    data class Tags(
        @field:Json(name = "name") val name: String,
        @field:Json(name = "name:sr-Latn") val nameSrLatn: String,
    )
}

internal data class Line(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "tags") val tags: Tags,
    @field:Json(name = "members") val points: List<Points>,
) {
    data class Tags(
        @field:Json(name = "name") val name: String,
        @field:Json(name = "name:sr-Latn") val nameSrLatn: String?,
    )

    data class Points(
        @field:Json(name = "ref") val ref: String,
        @field:Json(name = "type") val role: String,
    )
}

internal data class NodeResponse(
    @field:Json(name = "elements") val nodes: List<Node>,
)

internal data class Node(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "lat") val lat: Double,
    @field:Json(name = "lon") val lon: Double,
    @field:Json(name = "tags") val tags: Tags,
) {
    data class Tags(
        @field:Json(name = "gtfs:stop_id") val stopId: String,
        @field:Json(name = "name") val name: String,
        @field:Json(name = "name:sr-Latn") val nameSrLatn: String,
    )
}

internal interface OpenStreetMapApiService {
    @FormUrlEncoded
    @POST("/api/interpreter")
    suspend fun getOpenStreetMapDataNodes(
        @Field("data") data: String,
    ): NodeResponse

    @FormUrlEncoded
    @POST("/api/interpreter")
    suspend fun getOpenStreetMapDataLines(
        @Field("data") data: String,
    ): LineResponse
}

enum class City {
    BELGRADE,
    ;

    override fun toString(): String {
        return when (this) {
            BELGRADE -> "ГСП Београд"
        }
    }
}

internal class OpenStreetMapApi internal constructor(private val retrofitClient: OpenStreetMapApiService) {
    suspend fun getAllBusStops(city: City = City.BELGRADE): Result<List<BusStopResponse>, Error> {
        val response = retrofitClient.getOpenStreetMapDataNodes(makeStopsQuery(city))
        return Result.success(
            response.nodes.map {
                BusStopResponse(
                    it.id,
                    it.tags.stopId,
                    it.tags.nameSrLatn.ifBlank { it.tags.name },
                    it.lat,
                    it.lon,
                )
            },
        )
    }

    suspend fun getAllBusLines(city: City = City.BELGRADE): Result<List<BusLineResponse>, Error> {
        val response = retrofitClient.getOpenStreetMapDataLines(makeLinesQuery(city))
        return response.lines.map {
            BusLineResponse(
                it.id,
                it.tags.nameSrLatn ?: it.tags.name,
                it.points.map { point -> point.ref },
            )
        }.let { Result.success(it) }
    }

    companion object {
        private const val BASE_URL = "https://overpass-api.de/"

        private fun makeStopsQuery(city: City): String {
            return "[out:json];node[\"operator\"=\"${city}\"][\"gtfs:stop_id\"];\nout body;"
        }

        private fun makeLinesQuery(city: City): String {
            return "[out:json];relation[\"operator\"=\"${city}\"][\"type\"=\"route\"];\nout body;"
        }

        fun create(): OpenStreetMapApi {
            return OpenStreetMapApi(
                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                    .create(),
            )
        }
    }
}
