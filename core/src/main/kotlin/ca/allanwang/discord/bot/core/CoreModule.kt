package ca.allanwang.discord.bot.core

import com.gitlab.kordlib.core.Kord
import dagger.Module
import dagger.Provides
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.inject.Named

interface BotFeature {

    suspend fun init(): Boolean = true

    suspend fun Kord.attach()
}

@Module
object CoreModule {
    @Provides
    @Named("privPropertiesPath")
    @JvmStatic
    fun propertiesPath(): String = "priv.properties"

    @Provides
    @JvmStatic
    @Named("privProperties")
    fun properties(@Named("privPropertiesPath") privPropertiesPath: String): Properties {
        val prop = Properties()
        val file = File(privPropertiesPath)
        if (!file.isFile) return prop
        FileInputStream(file).use { prop.load(it) }
        return prop
    }
}