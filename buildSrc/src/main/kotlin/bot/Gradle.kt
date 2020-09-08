package bot

import org.gradle.api.JavaVersion

object Gradle {
    val jvmTarget: String = JavaVersion.VERSION_1_8.toString()

    private const val inlineClasses = "-XXLanguage:+InlineClasses"
    private const val coroutines = "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    private const val time = "-Xopt-in=kotlin.time.ExperimentalTime"
    private const val stdLib = "-Xopt-in=kotlin.ExperimentalStdlibApi"
    private const val optIn = "-Xopt-in=kotlin.RequiresOptIn"

    val compilerArgs: List<String> = listOf(
        coroutines,
        inlineClasses
    )

}