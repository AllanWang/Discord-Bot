package ca.allanwang.discord.bot.maps

import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.TimeZoneApi
import com.google.maps.model.GeocodingResult
import com.google.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

interface MapsApi {

    suspend fun getGeocode(query: String): Array<GeocodingResult>

    suspend fun getTimezone(latLng: LatLng): TimeZone?
}

class MapsApiImpl @Inject constructor(
    private val geoApiContext: GeoApiContext
) : MapsApi {

    override suspend fun getGeocode(query: String): Array<GeocodingResult> = withContext(Dispatchers.IO) {
        GeocodingApi.geocode(geoApiContext, query).awaitIgnoreError() ?: emptyArray()
    }

    override suspend fun getTimezone(latLng: LatLng): TimeZone? = withContext(Dispatchers.IO) {
        TimeZoneApi.getTimeZone(geoApiContext, latLng).awaitIgnoreError()
    }
}
