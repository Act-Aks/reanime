# Project guidelines

## Purpose and scope

Build an unofficial, fully native Android client for Re:ANIME using Kotlin.
The display name is **ReAnime** and release application ID is **com.reanime.app**.
Debug builds use **com.reanime.app.debug**. The public GitHub repository is
**Act-Aks/reanime**. Do not change remotes, publish releases, or push without authorization.
Re:ANIME is the only catalogue source. Alternate verified Re:ANIME domains are
mirrors, not additional providers. Take interaction inspiration from Nuvio,
Cloudstream, and similar apps without copying their extension architectures.

Read `docs/ARCHITECTURE.md` before changing architecture or infrastructure.
Keep it current when a decision changes. Do not silently substitute libraries.

Version-one requirements include native streaming, offline downloads, a local
library, and GitHub Releases distribution with stable/beta in-app updates.
Source playback and download compatibility still require a feasibility proof.
Never represent planned or unverified capabilities as working features.

## Approved stack

- Kotlin; Android-only Gradle modules, not a Kotlin Multiplatform project.
- Jetpack Compose, Material 3, and an app-owned design system.
- MVI presentation using AndroidX ViewModel, Coroutines, and StateFlow.
- AndroidX Navigation 3, not Navigation 2/NavController.
- Ktor Client 3 and kotlinx.serialization; no Retrofit.
- Fleeksoft Ksoup (`com.fleeksoft.ksoup:ksoup`) for HTML parsing; no Jsoup.
- Koin for dependency injection; no Hilt.
- AndroidX Room 3 (`androidx.room3`), KSP, and an explicitly configured SQLite driver.
- DataStore for preferences.
- Latest compatible stable Coil 3 with `coil-network-ktor3`.
- AndroidX Media3 for playback, media sessions, and offline media downloads.
- WorkManager only for suitable deferred jobs, not media playback/download loops.
- Gradle Kotlin DSL, version catalog, convention plugins, ktlint, Android Lint,
  and Kotlin-compatible Detekt.

Use compatible stable versions by default. Pin versions; no dynamic `+` versions.
Verify current release documentation and toolchain compatibility before pinning.
The Compose Styles API is an isolated evaluation, not an approved app-wide
dependency. Koin's experimental Navigation 3 registration API is not required.

## Architecture and dependencies

- Follow the module boundaries and dependency direction in the architecture doc.
- Keep external DTOs, HTML nodes, database entities, and Media3 implementation
  details out of screen contracts.
- Screens depend on feature state and callbacks, not repositories or HTTP clients.
- Repositories map remote/local representations into app-owned models.
- Keep Re:ANIME request/parsing logic inside the source integration boundary.
- Use focused use cases where they add business logic; do not wrap every call.
- Prefer constructor injection. Resolve Koin dependencies at composition roots
  and feature entry points, not from domain/business logic.
- Avoid speculative extension frameworks, generic base classes, and needless modules.

## MVI rules

Each feature uses `<Feature>ScreenState`, `<Feature>ScreenAction`, and
`<Feature>ScreenViewModel`, with pure state transitions. UI input is an Action;
`<Feature>ScreenEvent` is reserved for transient outgoing UI events when needed.
Use internal results where justified. Do not introduce parallel UiState/UiIntent/
UiEffect naming or empty event contracts for features with no outgoing events.

- Expose read-only `StateFlow<FeatureScreenState>` and render immutable state.
- Never mutate collections already published in state.
- Keep reducers pure: no I/O, coroutine launches, player calls, or DI lookups.
- Collect Compose state with lifecycle awareness.
- UI sends ScreenActions; it never implements source or persistence logic.
- Define concurrency per operation. Cancel superseded searches and prevent stale
  responses from replacing newer results. Serialize mutations where necessary.
- Preserve `CancellationException`; do not turn cancellation into an error state.
- Durable outcomes belong in state/persistence, not lossy one-off events.
- Document ScreenEvent delivery semantics; channels/flows do not guarantee exactly-once
  delivery across lifecycle changes or process death.
- Navigation is owned by the app/screen boundary. ViewModels do not own Android
  activities, navigation displays, or package installers.
- Save small restorable input through `SavedStateHandle`; store durable library
  and progress data in Room/DataStore.
- Do not dispatch every player/download progress tick through a global reducer.
- Do not introduce an MVI framework or a large base ViewModel without approval.

## Compose and navigation

- Use app-owned color, typography, spacing, shape, and motion tokens.
- Prefer reusable accessible components over repeated styling at call sites.
- Keep modifier parameters and state-hoisting conventions consistent.
- Use stable item identities, appropriate artwork sizes, and meaningful semantics.
- Support font scaling, TalkBack, system insets, cutouts, and reduced motion.
- Use serializable Navigation 3 keys containing IDs, not complete models or stream URLs.
- Scope ViewModels and saveable state to navigation entries; preserve tab stacks.
- Test predictive back, repeated detail entries, rotation, and process restoration.
- Keep experimental Styles usage, if approved after evaluation, inside the design system.

## Networking, parsing, and mirrors

- Fetch with configured Ktor clients, then parse HTML strings with Ksoup core.
- Prefer permitted structured endpoints when confirmed. Embedded JavaScript
  objects are not automatically JSON and Ksoup does not execute JavaScript.
- Never execute arbitrary website scripts as a parsing shortcut.
- Set explicit timeouts, bounded retries, response validation, and backoff.
- Retry only safe operations under a documented policy; honor rate limits.
- Restrict source URLs, redirects, and credential attachment by host/purpose.
- Never forward cookies/authorization indiscriminately to mirrors, artwork hosts,
  playback hosts, redirects, or GitHub.
- Keep identifiers independent of domains. Verify identity before mirror fallback.
- Separate offline, timeout, rate limit, blocked access, schema changes, missing
  sources, expired URLs, and playback/decoder failures.
- No access-control, anti-bot, or DRM bypasses. Verify terms and distribution rights.

## Playback and downloads

- Media3 owns playback and download execution; MVI observes and controls it.
- Player/service lifetime must be explicit, never tied to incidental recomposition.
- Treat Media3's DownloadIndex as authoritative for download execution state.
- Room stores associated catalogue/library metadata, not a competing download engine.
- Separate disposable streaming cache from explicitly retained offline media.
- Use app-specific storage initially; do not request broad storage access.
- Support queueing, track selection where available, pause/resume/retry/cancel,
  network requirements, concurrency limits, storage accounting, and offline playback.
- Do not assume expired-source downloads can resume against an arbitrary fresh URL.
- Verify offline audio/subtitles and any DRM license requirements per supported host.
- Handle audio focus, interruptions, PiP, rotation, process death, low storage,
  foreground-service rules, and notification permission/visibility behavior.
- Never promise support for every source before verifying it.

## Persistence and security

- Export/version Room schemas and test migrations. Never destructively migrate
  user watch history, progress, or library data as a routine fallback.
- Do not persist signed playback URLs as episode identity.
- Define Android backup/data-extraction rules; exclude credentials and caches as needed.
- Redact tokens, cookies, signed URLs, and sensitive account data from diagnostics.
- Use Keystore-backed protection when persisted sensitive credentials require it.
- APK contents, resources, and BuildConfig are public to an APK recipient.
- Never commit keystores, signing keys/passwords, local SDK paths, or access tokens.
- Never disable TLS verification.

## Build and quality

- Centralize versions in `gradle/libs.versions.toml` and repositories in settings.
- Follow the `Act-Aks/kswallet` build-logic pattern: compiled `Plugin<Project>`
  classes under `build-logic/convention`, shared catalog helpers, versionless
  convention-plugin aliases, and SDK/JVM/application metadata in the catalog.
  Modules apply `alias(libs.plugins.reanime...)`; do not reintroduce precompiled
  script conventions or duplicate SDK/dependency versions in plugin classes.
- Check in the Gradle wrapper; pin a compatible JDK/toolchain locally and in CI.
- Use a small included `build-logic` build for repeated module conventions.
- Follow the selected AGP's Kotlin setup; do not blindly mix legacy Kotlin Android
  plugin configuration with newer built-in Kotlin support.
- Use debug, release, and a release-like benchmark build when benchmarks are added.
- Release builds use R8/resource shrinking with verified keep rules.
- `.editorconfig` is the formatting source of truth. Pin ktlint plugin/engine versions.
- Exclude generated/build output from formatting; do not exclude handwritten code.
- Formatting is explicit. CI checks formatting; it does not rewrite files.
- Keep Detekt rules focused; avoid duplicate formatting enforcement.
- No new warning baselines or disabled checks simply to make CI pass.

### Expected verification commands

These are required task contracts for the build setup, not claims that a scaffold
already exists. Once available, run the relevant commands from the repository root:

```sh
./gradlew ktlintCheck
./gradlew detekt
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Formatting is applied explicitly with `./gradlew ktlintFormat`.
CI must additionally run JVM tests in non-Android modules and checks for included
build logic. Add instrumented migration/UI tests and release/benchmark checks as
their modules land, and document their exact invocations.

## GitHub releases and updates

- Use Conventional Commits (`type(scope): summary`) as documented in CONTRIBUTING.md.
  PR titles must follow the same convention so future squash-merge release analysis
  sees the correct type. Do not create release tags or push without authorization.

- Distribute signed universal APKs through public GitHub Releases initially.
- One installed application ID/signing identity spans stable and beta channels.
- Beta is opt-in. Exclude drafts; stable excludes prereleases. Compare integer
  version codes, not lexically sorted version names or publication dates alone.
- Version codes increase globally across beta, stable, and hotfix releases.
- Switching channel never silently downgrades or requires uninstalling user data.
- Separate untrusted PR CI from protected release/signing workflows.
- Use readable major-version action tags (for example `actions/checkout@v6`),
  not commit SHA references, as requested by the project owner. These tags are
  mutable; use Dependabot to review major-version upgrades. Keep Gradle distribution
  checksums and other artifact-integrity verification in place.
- Use minimal permissions, protected environments, and
  concurrency controls. Do not expose release secrets to PR-controlled code.
- Build/upload a draft release, verify all artifacts, then publish.
- Publish APK, signed update manifest, detached manifest signature, checksums,
  and release notes. Do not embed a GitHub access token in the app.
- Verify manifest signature, eligibility, SHA-256, APK identity/version, and
  expected signing identity before handing installation to Android.
- Manifest signing uses a separate key from APK signing, with a rotation plan.
- Installation needs user consent; no silent or unsolicited background install.
- Back up signing keys securely outside GitHub; never print them in logs.

## Tests and definition of done

- Test MVI reducers, concurrency, restoration, and effect behavior.
- Test Ktor error handling with MockEngine and parsers against local fixtures.
- Keep live source checks small, permitted, rate-limited, and outside deterministic PR tests.
- Test Room migrations, Koin wiring, navigation entry scoping, and key UI flows.
- Test playback/downloads on physical devices as well as emulators.
- Test signed upgrades, beta/stable eligibility, tampering, cancellation, and data retention.
- Add accessibility/performance coverage as the UI develops.
- A change is done when relevant checks pass, errors/security are handled,
  documentation matches behavior, and unrelated work is preserved.
- Report exactly what was tested and what could not be tested. Never invent passing
  checks, working streams, compatible downloads, or completed release infrastructure.
