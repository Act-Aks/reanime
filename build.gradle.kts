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
        ":core:designsystem:testDebugUnitTest",
        ":feature:browse:testDebugUnitTest",
        ":app:assembleDebug"
    )
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}

tasks.register("verifyRelease") {
    group = "verification"
    description = "Checks release lint and R8/resource shrinking without signing or publishing."
    dependsOn(":app:lintRelease", ":app:assembleRelease")
}
