plugins {
    alias(libs.plugins.reanime.kotlin.library)
}

dependencies {
    api(projects.core.model)
    api(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.koin.core)
    implementation(libs.coroutines.core)
    testImplementation(libs.ktor.client.mock)
}
