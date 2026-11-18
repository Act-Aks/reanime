plugins {
    alias(libs.plugins.reanime.kotlin.library)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(libs.ksoup)
    implementation(libs.coroutines.core)
}

tasks.register<JavaExec>("probeCatalogue") {
    group = "verification"
    description = "Opt-in, single-request live catalogue probe; never part of ordinary tests or CI."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.reanime.source.reanime.LiveCatalogueProbe")
}

tasks.register<JavaExec>("probeDetails") {
    group = "verification"
    description = "Opt-in, two-request live details/episode-metadata probe; not part of CI."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.reanime.source.reanime.LiveDetailsProbe")
    args(providers.gradleProperty("probeAnimeId").orElse("red-river-nb73wr").get())
}
