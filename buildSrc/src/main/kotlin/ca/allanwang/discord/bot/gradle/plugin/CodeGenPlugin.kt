package ca.allanwang.discord.bot.gradle.plugin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CodeGenPlugin : Plugin<Project> {

    private fun TypeSpec.Builder.constString(name: String, value: String?) = addProperty(
        PropertySpec.builder(name, String::class, KModifier.CONST)
            .initializer("%S", value).build()
    )

    private fun TypeSpec.Builder.constBool(name: String, value: Boolean) = addProperty(
        PropertySpec.builder(name, Boolean::class, KModifier.CONST)
            .initializer("%L", value).build()
    )

    override fun apply(target: Project) {
        val file = buildFile(target)
        val dir = File(target.buildDir, "plugingen")
        file.writeTo(dir)
    }

    data class BuildData(val version: String, val buildTime: String, val commitUrl: String)

    private fun buildData(target: Project): BuildData {
        val git = Git.wrap(FileRepositoryBuilder.create(File(target.rootDir, ".git")))
        val headRef = git.repository.findRef(Constants.HEAD).target
        val hash = headRef.objectId.abbreviate(8).name()
        val now = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm:ss a").format(ZonedDateTime.now())
        val originUrl = git.originUrl()
        return BuildData(version = hash, buildTime = now, commitUrl = "$originUrl/commit/${headRef.objectId.name()}")
    }

    private fun buildFile(target: Project): FileSpec {
        val buildData = runCatching { buildData(target) }.onFailure { it.printStackTrace() }.getOrNull()
        return FileSpec.builder("ca.allanwang.discord.bot.gradle", "Build")
            .addType(
                TypeSpec.objectBuilder("Build")
                    .constBool("valid", buildData != null)
                    .constString("version", buildData?.version)
                    .constString("buildTime", buildData?.buildTime)
                    .constString("commitUrl", buildData?.commitUrl)
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