package com.reanime.convention.internal

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.version(alias: String): String = versionCatalog.findVersion(alias)
    .orElseThrow { GradleException("Missing version '$alias' in libs.versions.toml") }
    .requiredVersion
    .takeIf(String::isNotBlank)
    ?: throw GradleException("Version '$alias' must declare a required value")

internal fun Project.intVersion(alias: String): Int = version(alias).toIntOrNull()
    ?: throw GradleException("Version '$alias' must be an integer")

internal fun Project.lib(alias: String): Provider<MinimalExternalModuleDependency> = versionCatalog.findLibrary(alias)
    .orElseThrow { GradleException("Missing library '$alias' in libs.versions.toml") }

internal fun Project.pluginId(alias: String): String = versionCatalog.findPlugin(alias)
    .orElseThrow { GradleException("Missing plugin '$alias' in libs.versions.toml") }
    .get().pluginId

// Resolves IDs from the catalog; plugin artifacts/versions must be on the classpath.
internal fun Project.applyPlugins(vararg aliases: String) {
    aliases.forEach { pluginManager.apply(pluginId(it)) }
}

internal fun Project.implementation(alias: String) {
    dependencies.add("implementation", lib(alias))
}
