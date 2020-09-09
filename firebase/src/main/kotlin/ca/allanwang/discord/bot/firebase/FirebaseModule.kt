package ca.allanwang.discord.bot.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import java.io.FileInputStream
import javax.inject.Named
import javax.inject.Singleton

@Module
object FirebaseModule {
    @Provides
    @JvmStatic
    @Named("firebaseFilePath")
    fun firebaseFilePath(): String = "files/discord-bot-firebase.json"

    @Provides
    @JvmStatic
    @Named("firebaseUrl")
    fun firebaseUrl(): String = "https://discord-bot-3df7d.firebaseio.com"

    @Provides
    @JvmStatic
    @Singleton
    fun firebaseApp(
        @Named("firebaseFilePath") firebaseFilePath: String,
        @Named("firebaseUrl") firebaseUrl: String
    ): FirebaseApp {
        val serviceAccount = FileInputStream(firebaseFilePath)

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl(firebaseUrl)
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Provides
    @JvmStatic
    @Singleton
    fun firebaseDatabase(firebaseApp: FirebaseApp) = FirebaseDatabase.getInstance(firebaseApp)
}