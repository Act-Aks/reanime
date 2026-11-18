package com.reanime.convention

import com.android.build.api.dsl.LibraryExtension
import com.reanime.convention.internal.applyPlugins
import com.reanime.convention.internal.configureAndroid
import com.reanime.convention.internal.configureKotlinToolchain
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugins("android-library", "reanime-kotlin-quality")
        extensions.configure<LibraryExtension> { configureAndroid(target) }
        configureKotlinToolchain()
    }
}
