plugins {
    alias(libs.plugins.reanime.android.library)
    alias(libs.plugins.reanime.android.compose)
    alias(libs.plugins.reanime.android.compose.di)
    alias(libs.plugins.reanime.android.testing)
}

android { namespace = "com.reanime.feature.browse" }

dependencies {
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.core)
}
