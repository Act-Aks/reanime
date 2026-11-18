# ReAnime

A native Kotlin Android client for Re:ANIME. **This repository currently contains
the application/build foundation, not a working streaming client.** Catalogue
access, playback, downloads, persistence, and in-app updates are not connected yet.

## Requirements

- JDK matching `projectJavaVersion` (currently 17), available through `JAVA_HOME`
  or your IDE's Gradle JDK setting.
- A dependency-compatible Android SDK and the build tools required by the catalog's
  AGP version (currently 9.5.1); see the current compile-SDK blocker below.
- Android Studio with support for the selected AGP, or the command-line SDK tools.
- Android SDK path through `ANDROID_HOME` or an untracked `local.properties` file.

Use the checked-in wrapper; a system Gradle installation is not required. Current
SDK/JVM/application settings live in `gradle/libs.versions.toml`. The initial
Android minimum is API 26.

**Current checkout validation:** dependencies/tooling were upgraded after the
initial successful foundation check. `verifyFoundation` currently fails AAR
metadata checks because Compose 1.12.1/Lifecycle 2.11.0 require compile SDK 37+
while `projectCompileSdkVersion` is 36. Resolve this compatibility mismatch before
expecting GitHub CI to pass. The earlier verified baseline used JDK 21/AGP 9.2.1;
the catalog now selects JVM 17/AGP 9.5.1 and the wrapper selects Gradle 9.9.1.
Those upgrades are preserved, not reverted.

## Build and verification

```sh
./gradlew verifyFoundation
./gradlew :app:assembleDebug
```

`verifyFoundation` checks root/module formatting, isolated Detekt, convention-plugin
validation, browse unit tests, Android lint (including dependencies), and debug
assembly. Included build logic is checked too. There are not yet instrumented
UI/device tests or Gradle TestKit tests.

Format explicitly before running checks; do not run formatting and verification
concurrently:

```sh
./gradlew ktlintFormat :build-logic:ktlintFormat :build-logic:convention:ktlintFormat
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk` and uses
`com.reanime.app.debug`. The eventual release package is `com.reanime.app`.
Release signing/publishing is not configured; do not distribute unsigned release
builds as official updates.

## Structure

```text
app/                    Application, Koin setup, Navigation 3 entry points
core/designsystem/      Initial Compose theme
feature/browse/         Foundation screen, MVI contracts, reducer/ViewModel tests
build-logic/convention/ Compiled, catalog-driven Gradle convention plugins
gradle/                 Version catalog and checksum-pinned Gradle wrapper
.github/workflows/      PR/push verification workflow (runs once hosted on GitHub)
```

MVI contracts use `FeatureScreenState`, `FeatureScreenAction`,
`FeatureScreenViewModel`, and optional transient `FeatureScreenEvent`. The
foundation screen deliberately reports that the source is not connected rather
than displaying fabricated catalogue data.

## Project references

- [Agent/contributor guidelines](AGENTS.md)
- [Commit and contribution conventions](CONTRIBUTING.md)
- [Architecture and remaining decisions](docs/ARCHITECTURE.md)
- [Build-logic maintenance](build-logic/README.md)

No GitHub repository has been created/configured yet. The workflow uses repository
context rather than invented release coordinates. GitHub publishing, signing
secrets, and stable/beta update infrastructure remain future milestones.
