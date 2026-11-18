package com.reanime.convention

import com.android.build.api.dsl.Lint
import com.reanime.convention.internal.applyPlugins
import com.reanime.convention.internal.intVersion
import com.reanime.convention.internal.lib
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugins("kotlin-jvm", "java-library", "android-lint", "reanime-kotlin-quality")
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(intVersion("projectJavaVersion"))
        }
        dependencies.add("testImplementation", lib("junit"))
        dependencies.add("testImplementation", lib("coroutines-test"))
        extensions.configure<Lint> {
            warningsAsErrors = true
            informational += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        }
    }
}
