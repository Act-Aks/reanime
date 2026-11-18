plugins {
    alias(libs.plugins.reanime.kotlin.quality)
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register("verifyFoundation") {
    group = "verification"
    description = "Checks formatting, static analysis, tests, Android lint, and debug assembly."
    dependsOn(
        "ktlintCheck",
        "detekt",
        ":app:ktlintCheck",
        ":app:detekt",
        ":core:designsystem:ktlintCheck",
        ":core:designsystem:detekt",
        ":feature:browse:ktlintCheck",
        ":feature:browse:detekt",
        ":app:lintDebug",
        ":app:testDebugUnitTest",
        ":feature:browse:testDebugUnitTest",
        ":app:assembleDebug"
    )
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
