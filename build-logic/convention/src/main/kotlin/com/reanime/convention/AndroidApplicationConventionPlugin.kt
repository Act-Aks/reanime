package com.reanime.convention

import com.android.build.api.dsl.ApplicationExtension
import com.reanime.convention.internal.applyPlugins
import com.reanime.convention.internal.configureAndroid
import com.reanime.convention.internal.configureKotlinToolchain
import com.reanime.convention.internal.intVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugins("android-application", "reanime-kotlin-quality")
        extensions.configure<ApplicationExtension> {
            configureAndroid(target)
            defaultConfig.targetSdk = intVersion("projectTargetSdkVersion")
            lint.checkDependencies = true
        }
        configureKotlinToolchain()
    }
}
