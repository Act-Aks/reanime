plugins {
    alias(libs.plugins.reanime.android.library)
    alias(libs.plugins.reanime.android.compose)
}

android { namespace = "com.reanime.core.designsystem" }

dependencies {
    implementation(libs.compose.material3)
}
