plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(libs.versions.ktlint.asProvider())
    filter {
        exclude { element ->
            val path = element.file.invariantSeparatorsPath
            path.contains("/build/") || path.contains("/.gradle/")
        }
    }
}

tasks.register("check") {
    description = "Build logic verification"
    group = "verification"
    dependsOn("ktlintCheck", ":convention:check")
}
