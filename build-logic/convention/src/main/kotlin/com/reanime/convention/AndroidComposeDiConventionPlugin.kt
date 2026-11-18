package com.reanime.convention

import com.reanime.convention.internal.implementation
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidComposeDiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        implementation("koin-android")
        implementation("koin-compose")
    }
}
