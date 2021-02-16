package ca.allanwang.discord.bot.maps

import ca.allanwang.discord.bot.core.CoreModule
import ca.allanwang.discord.bot.core.PrivProperties
import com.google.maps.GeoApiContext
import dagger.Binds
import dagger.Module
import dagger.Provides
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [MapsCoreModule::class, MapsApiModule::class])
object MapsModule

@Module(includes = [CoreModule::class])
object MapsCoreModule {
    @Provides
    @JvmStatic
    @Named("mapsApiPropKey")
    fun mapsApiPropKey() = "google_api_key"

    @Provides
    @JvmStatic
    @Singleton
    @Named("mapsApiKey")
    fun mapsApiKey(
        @Named("mapsApiPropKey") mapsApiPropKey: String,
        @PrivProperties privProperties: Properties
    ): String = privProperties.getProperty(mapsApiPropKey) ?: error("maps api key not found")

    @Provides
    @JvmStatic
    @Singleton
    fun geocodingApi(@Named("mapsApiKey") mapsApiKey: String): GeoApiContext {
        return GeoApiContext.Builder()
            .apiKey(mapsApiKey)
            .build()
    }
}

@Module
interface MapsApiModule {
    @Binds
    fun to(mapsApi: MapsApiImpl): MapsApi
}
