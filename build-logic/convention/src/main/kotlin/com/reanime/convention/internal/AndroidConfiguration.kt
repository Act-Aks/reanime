package com.reanime.convention.internal

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun CommonExtension.configureAndroid(project: Project) {
    compileSdk = project.intVersion("projectCompileSdkVersion")
    defaultConfig.minSdk = project.intVersion("projectMinSdkVersion")
    compileOptions.apply {
        sourceCompatibility = JavaVersion.toVersion(project.intVersion("projectJavaVersion"))
        targetCompatibility = JavaVersion.toVersion(project.intVersion("projectJavaVersion"))
    }
    lint.warningsAsErrors = true
    // Keep upgrade advice visible without making deterministic CI depend on
    // whether an upstream author has published a release since our last build.
    lint.informational += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
}

internal fun Project.configureKotlinToolchain() {
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(intVersion("projectJavaVersion"))
    }
}
