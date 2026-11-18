plugins {
    alias(libs.plugins.reanime.kotlin.library)
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ksoup)
    implementation(libs.coroutines.core)
    testImplementation(libs.ktor.client.mock)
}

tasks.register<JavaExec>("probeCatalogue") {
    group = "verification"
    description = "Opt-in, single-request live catalogue probe; never part of ordinary tests or CI."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.reanime.source.reanime.LiveCatalogueProbe")
}
