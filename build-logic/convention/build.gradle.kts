plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

group = "com.reanime.buildlogic"

kotlin {
    jvmToolchain(libs.versions.projectJavaVersion.get().toInt())
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.reanime.android.application.get().pluginId
            implementationClass = "com.reanime.convention.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.reanime.android.library.get().pluginId
            implementationClass = "com.reanime.convention.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.reanime.android.compose.asProvider().get().pluginId
            implementationClass = "com.reanime.convention.AndroidComposeConventionPlugin"
        }
        register("androidComposeDi") {
            id = libs.plugins.reanime.android.compose.di.get().pluginId
            implementationClass = "com.reanime.convention.AndroidComposeDiConventionPlugin"
        }
        register("androidTesting") {
            id = libs.plugins.reanime.android.testing.get().pluginId
            implementationClass = "com.reanime.convention.AndroidTestingConventionPlugin"
        }
        register("kotlinQuality") {
            id = libs.plugins.reanime.kotlin.quality.get().pluginId
            implementationClass = "com.reanime.convention.KotlinQualityConventionPlugin"
        }
        register("kotlinLibrary") {
            id = libs.plugins.reanime.kotlin.library.get().pluginId
            implementationClass = "com.reanime.convention.KotlinLibraryConventionPlugin"
        }
    }
}

ktlint {
    version.set(libs.versions.ktlint.asProvider())
    filter {
        exclude { element ->
            val path = element.file.invariantSeparatorsPath
            path.contains("/build/") || path.contains("/.gradle/")
        }
    }
}
