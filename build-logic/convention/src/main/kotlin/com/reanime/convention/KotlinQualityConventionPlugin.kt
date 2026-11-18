package com.reanime.convention

import com.reanime.convention.internal.applyPlugins
import com.reanime.convention.internal.lib
import com.reanime.convention.internal.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class KotlinQualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugins("ktlint")
        extensions.configure<KtlintExtension> {
            version.set(target.version("ktlint"))
            filter {
                exclude { element ->
                    val path = element.file.invariantSeparatorsPath
                    path.contains("/build/") || path.contains("/.gradle/")
                }
            }
        }
        configureDetekt()
    }

    private fun Project.configureDetekt() {
        val cli = configurations.create("detektCli")
        dependencies.add(cli.name, lib("detekt-cli"))
        val paths = if (this == rootProject) {
            listOf("build.gradle.kts", "settings.gradle.kts", "build-logic")
        } else {
            listOf("src", "build.gradle.kts")
        }
        tasks.register<JavaExec>("detekt") {
            group = "verification"
            description = "Runs isolated, syntax-only Detekt analysis."
            classpath = cli
            mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
            args(
                "--input", paths.joinToString(",") { file(it).absolutePath },
                "--excludes", "**/build/**,**/.gradle/**",
                "--config", rootProject.file("config/detekt.yml").absolutePath,
                "--build-upon-default-config",
                "--report", "html:${layout.buildDirectory.file("reports/detekt/detekt.html").get().asFile}"
            )
        }
    }
}
