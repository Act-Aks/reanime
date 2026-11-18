# Catalog-driven build logic

This included build follows the compiled convention-plugin structure of
[`Act-Aks/kswallet`](https://github.com/Act-Aks/kswallet) reviewed at commit
`69f096d323f37f53460a8e9ca81597fea29cf758`.

## Single source of truth

`../gradle/libs.versions.toml` is imported as `libs` by this build's settings.
It defines:

- External library/plugin versions and plugin implementation dependencies.
- Compile/minimum/target SDK levels and JVM toolchain.
- Compile-SDK minor level and the Gradle runtime JDK (`projectBuildJavaVersion`),
  separately from the compilation toolchain (`projectJavaVersion`).
- Application display name, application ID, and development version metadata.
- Versionless `reanime-*` aliases for our convention plugins.

The Gradle wrapper version/checksum remains in its standard wrapper properties.
The CI workflow reads runtime/toolchain JDKs and the full compile-SDK version from
the catalog, installs the exact SDK package, and exposes both JDKs to Gradle.
Current settings are build JDK 21, toolchain 17, compile SDK 37.0, target 36, and
minimum 26. Release version overrides/allocation are not implemented;
the current catalog version is a development value, not a release allocator.

## How it works

1. Root settings include this build through `pluginManagement.includeBuild`.
2. `:convention` compiles ordinary Kotlin `Plugin<Project>` implementations.
3. `gradlePlugin` registrations read convention IDs from catalog aliases.
4. Internal helpers resolve required versions, integers, library providers, and
   plugin IDs with actionable errors for missing/invalid entries.
5. Modules apply `alias(libs.plugins.reanime.android.application)` and similar aliases.

Reading a plugin ID from the catalog does **not** resolve its version automatically.
External implementation artifacts must also be declared as catalog-backed
dependencies in `convention/build.gradle.kts`.

## Available conventions

| Alias | Responsibility |
| --- | --- |
| `reanime-android-application` | AGP application, SDK/JVM defaults, quality checks |
| `reanime-android-library` | AGP library, SDK/JVM defaults, quality checks |
| `reanime-android-compose` | Compose compiler/build feature and BOM |
| `reanime-android-compose-di` | Shared Koin Android/Compose dependencies |
| `reanime-android-testing` | JUnit and coroutine test dependencies |
| `reanime-kotlin-quality` | Pinned ktlint and isolated syntax-only Detekt |
| `reanime-kotlin-library` | JVM library, catalog JVM toolchain, JUnit/coroutine tests, quality checks, standalone Android Lint |

Apply the Android base plugin before Compose/DI/testing conventions. Keep feature
dependencies in their modules. Add Room/network conventions when those modules
actually exist; do not create unused plugin scaffolding.

Catalogue JVM modules (`core:model`, `core:network`, `core:data`, `source:reanime`) use the Kotlin
library convention, not Android library/Compose plugins. Their tests and lint are
explicitly included in `verifyFoundation`; live website probes are not.

## Adding or changing a convention

1. Add required dependency/version/plugin aliases to the root catalog.
2. Add the implementation artifact dependency to `:convention`, if needed.
3. Implement a focused class under `com.reanime.convention` using catalog helpers.
4. Register its ID from the catalog and its implementation class in `gradlePlugin`.
5. Apply the versionless alias in the consuming module.
6. Format, run `./gradlew verifyFoundation verifyRelease` from the repository root,
   and update docs. Run CI helper tests when changing SDK/JDK/package handling.

Never reintroduce precompiled `.gradle.kts` convention plugins or hardcoded SDK/
dependency versions inside plugin classes. Application-specific namespace and
release build behavior can remain in the application module.

## Quality policy

ktlint checks handwritten Kotlin and build scripts, excluding generated/build output.
Included build root/convention formatting and plugin validation are part of the
foundation gate. Formatting is explicit; CI never edits source.

Detekt 1.23.8 runs in an isolated CLI process because its stable Gradle integration
targets an older AGP/Kotlin combination. It is syntax-only: Kotlin compilation and
Android Lint are still required, and current Kotlin compiler type analysis is not claimed.

Android Lint warnings fail verification, except dependency/tool upgrade advice is
kept visible as informational (`GradleDependency`, `NewerVersionAvailable`, and
`AndroidGradlePluginVersion`). This avoids failing an unchanged commit merely
because a dependency author publishes a new version. Correctness/security/API
findings remain blocking; there is no lint baseline. Dependabot proposes upgrades
for review rather than silently changing the tested compatibility baseline.
