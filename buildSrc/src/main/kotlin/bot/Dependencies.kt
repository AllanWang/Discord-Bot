package bot

object Dependencies {
    const val kord = "dev.kord:kord-core:${Versions.kord}"
    const val kordxCommands = "com.gitlab.kordlib.kordx:kordx-commands-runtime-kord:${Versions.kordxCommands}"
    const val kordxCommandsKapt = "com.gitlab.kordlib.kordx:kordx-commands-processor:${Versions.kordxCommands}"
    const val kordxEmojis = "dev.kord.x:emoji:${Versions.kordxEmojis}"

    val slf4j = slf4j("api")
    fun slf4j(type: String) = "org.slf4j:slf4j-${type}:${Versions.slf4j}"

    val flogger = "com.google.flogger:flogger:${Versions.flogger}"
    fun flogger(type: String) = "com.google.flogger:flogger-${type}:${Versions.flogger}"

    fun firebase(type: String) = "com.google.firebase:firebase-${type}:${Versions.firebase}"

    const val jsoup = "org.jsoup:jsoup:${Versions.jsoup}"

    const val dagger = "com.google.dagger:dagger:${Versions.dagger}"
    const val daggerKapt = "com.google.dagger:dagger-compiler:${Versions.dagger}"

    // https://github.com/Kotlin/kotlinx.coroutines/releases
    val coroutines = coroutines("core")
    fun coroutines(type: String) = "org.jetbrains.kotlinx:kotlinx-coroutines-${type}:${Versions.coroutines}"

    const val googleMaps = "com.google.maps:google-maps-services:${Versions.googleMaps}"

    fun junit(type: String) = "org.junit.jupiter:junit-jupiter-${type}:${Versions.junit}"

    const val truth = "com.google.truth:truth:${Versions.truth}"
}