plugins {
    alias(libs.plugins.reanime.kotlin.library)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.source.reanime)
    implementation(libs.coroutines.core)
}
