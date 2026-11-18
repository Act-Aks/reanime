package com.reanime.convention

import com.reanime.convention.internal.lib
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidTestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        dependencies.add("testImplementation", lib("junit"))
        dependencies.add("testImplementation", lib("coroutines-test"))
    }
}
