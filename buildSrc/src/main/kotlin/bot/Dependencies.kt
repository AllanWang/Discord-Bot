package bot

object Dependencies {
    const val kord = "com.gitlab.kordlib.kord:kord-core:${Versions.kord}"

    val slf4j = slf4j("api")

    fun slf4j(type: String) = "org.slf4j:slf4j-${type}:${Versions.slf4j}"

    val flogger = "com.google.flogger:flogger:${Versions.flogger}"

    fun flogger(type: String) = "com.google.flogger:flogger-${type}:${Versions.flogger}"
}