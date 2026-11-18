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
        ":feature:home:ktlintCheck",
        ":feature:home:detekt",
        ":feature:details:ktlintCheck",
        ":feature:details:detekt",
        ":core:network:ktlintCheck",
        ":core:network:detekt",
        ":core:network:test",
        ":core:network:lint",
        ":core:model:ktlintCheck",
        ":core:model:detekt",
        ":core:model:test",
        ":core:model:lint",
        ":core:data:ktlintCheck",
        ":core:data:detekt",
        ":core:data:test",
        ":core:data:lint",
        ":source:reanime:ktlintCheck",
        ":source:reanime:detekt",
        ":source:reanime:test",
        ":source:reanime:lint",
        ":app:lintDebug",
        ":app:testDebugUnitTest",
        ":core:designsystem:testDebugUnitTest",
        ":feature:home:testDebugUnitTest",
        ":feature:details:testDebugUnitTest",
        ":app:assembleDebug"
    )
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}

tasks.register("verifyRelease") {
    group = "verification"
    description = "Checks release lint and R8/resource shrinking without signing or publishing."
    dependsOn(":app:lintRelease", ":app:assembleRelease")
}
