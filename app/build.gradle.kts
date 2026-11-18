plugins {
    alias(libs.plugins.reanime.android.application)
    alias(libs.plugins.reanime.android.compose)
    alias(libs.plugins.reanime.android.compose.di)
    alias(libs.plugins.reanime.android.testing)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.reanime.app"
    buildFeatures { resValues = true }
    defaultConfig {
        applicationId = libs.versions.projectApplicationId.get()
        versionCode = libs.versions.projectVersionCode.get().toInt()
        versionName = libs.versions.projectVersion.get()
        resValue("string", "app_name", libs.versions.projectApplicationName.get())
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.feature.browse)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.serialization.core)
}
