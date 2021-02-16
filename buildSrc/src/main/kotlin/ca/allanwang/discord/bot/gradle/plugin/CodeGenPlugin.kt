package ca.allanwang.discord.bot.gradle.plugin

import com.squareup.kotlinpoet.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
class CodeGenPlugin : Plugin<Project> {

    private fun FunSpec.Builder.stringParam(name: String, value: String?) = param(name, "%S", value)

    private fun FunSpec.Builder.boolParam(name: String, value: Boolean) = param(name, "%L", value)

    private inline fun <reified T> FunSpec.Builder.param(name: String, format: String, value: T) = addParameter(
        ParameterSpec.builder(name, T::class, KModifier.OVERRIDE, KModifier.PUBLIC).defaultValue(format, value).build()
    )

    private inline fun <reified T> TypeSpec.Builder.override(name: String) =
        addProperty(PropertySpec.builder(name, T::class, KModifier.OVERRIDE).initializer(name).build())

    override fun apply(target: Project) {
        val file = buildFile(target)
        val dir = File(target.buildDir, "plugingen")
        dir.delete()
        file.writeTo(dir)
    }

    data class BuildData(
        val valid: Boolean = false,
        val version: String = "",
        val buildTime: String = "",
        val commitUrl: String = ""
    )

    private fun buildData(target: Project): BuildData {
        val git = Git.wrap(FileRepositoryBuilder.create(File(target.rootDir, ".git")))
        val headRef = git.repository.findRef(Constants.HEAD).target
        val hash = headRef.objectId.abbreviate(8).name()
        val now = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm:ss a").format(ZonedDateTime.now())
        val originUrl = git.originUrl()
        return BuildData(
            valid = true,
            version = hash,
            buildTime = now,
            commitUrl = "$originUrl/tree/${headRef.objectId.name()}"
        )
    }

    private fun buildFile(target: Project): FileSpec {
        val buildData = runCatching { buildData(target) }.onFailure { it.printStackTrace() }.getOrNull() ?: BuildData()

        fun KProperty1<BuildData, *>.parameter() =
            ParameterSpec.builder(name, returnType.asTypeName()).defaultValue(
                when (returnType) {
                    typeOf<String>() -> "%S"
                    typeOf<Boolean>() -> "%L"
                    else -> throw IllegalArgumentException("Unsupported return type $returnType")
                }, get(buildData)
            ).build()

        fun KProperty1<BuildData, *>.property() =
            PropertySpec.builder(name, returnType.asTypeName(), KModifier.OVERRIDE).initializer(name).build()

        val properties = buildData::class.memberProperties.map { it as KProperty1<BuildData, *> }

        return FileSpec.builder("ca.allanwang.discord.bot.gradle", "GitBuild")
            .addType(
                TypeSpec.classBuilder("GitBuild")
                    .addModifiers(KModifier.DATA)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameters(properties.map { it.parameter() })
                            .build()
                    )
                    .addProperties(properties.map { it.property() })
                    .addSuperinterface(ClassName("ca.allanwang.discord.bot.core", "Build"))
                    .build()
            )
            .build()
    }

    private fun Git.originUrl(): String {
        var url = repository.config.getString("remote", "origin", "url")
        url = url.substringBeforeLast(".git")
        url = url.replace("git@github.com:", "https://github.com/")
        return url
    }

}