package ca.allanwang.discord.bot.firebase

import ca.allanwang.discord.bot.core.PrivProperties
import com.gitlab.kordlib.core.Kord
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import java.io.FileInputStream
import java.util.*
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
    @Named("firebaseUrlKey")
    fun firebaseUrlKey(): String = "firebase_url"

    @Provides
    @JvmStatic
    @Named("firebaseUrl")
    fun firebaseUrl(
        @Named("firebaseUrlKey") firebaseUrlKey: String,
        @PrivProperties privProperties: Properties
    ): String = privProperties.getProperty(firebaseUrlKey) ?: error("firebase url not found")

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
    fun firebaseDatabase(firebaseApp: FirebaseApp): FirebaseDatabase = FirebaseDatabase.getInstance(firebaseApp)

    @Provides
    @FirebaseRootRef
    @JvmStatic
    fun firebaseRootRef(firebaseDatabase: FirebaseDatabase, kord: Kord): DatabaseReference = firebaseDatabase.reference.child(kord.selfId.value)
}