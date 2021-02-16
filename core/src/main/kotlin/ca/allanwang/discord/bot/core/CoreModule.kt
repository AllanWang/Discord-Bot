package ca.allanwang.discord.bot.core

import ca.allanwang.discord.bot.gradle.GitBuild
import dagger.Module
import dagger.Provides
import dev.kord.core.Kord
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.inject.Named
import javax.inject.Qualifier

interface BotFeature {

    suspend fun init(): Boolean = true

    suspend fun Kord.attach()
}

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class PrivProperties

@Module
object CoreModule {
    @Provides
    @Named("privPropertiesPath")
    @JvmStatic
    fun propertiesPath(): String = "priv.properties"

    @Provides
    @JvmStatic
    @PrivProperties
    fun properties(@Named("privPropertiesPath") privPropertiesPath: String): Properties {
        val prop = Properties()
        val file = File(privPropertiesPath)
        if (!file.isFile) return prop
        FileInputStream(file).use { prop.load(it) }
        return prop
    }

    @Provides
    @JvmStatic
    fun build(): Build = GitBuild()
}
