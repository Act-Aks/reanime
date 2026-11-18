# Re:ANIME Android architecture

Status: minimal foundation and CI refinements locally verified; hosted CI rerun
pending push. Source/media features remain unverified.

This document records agreed requirements, recommended implementation choices,
and outstanding validation. `AGENTS.md` provides day-to-day contribution rules.

## 1. Product and scope

Build an unofficial native Kotlin Android application for Re:ANIME, not a WebView
wrapper. Use one catalogue source with verified alternate domains. Inspiration
from other streaming clients concerns interaction and polish, not a multi-provider
plugin system.

### Confirmed application identity

- Display name: **ReAnime**.
- Release application ID: **com.reanime.app**.
- Debug application ID: **com.reanime.app.debug**.
- GitHub repository: `https://github.com/Act-Aks/reanime` (public, default branch `main`).
  Release publishing/signing and update discovery remain unconfigured.
- Application naming does not establish permission to use website branding/assets.

### Version-one requirements

- Home/catalogue, search, details, and episode browsing.
- Native streaming with available server, quality, audio, and subtitle selection.
- Local watchlist, history, resume progress, and continue watching.
- PiP and appropriate Android media controls.
- Offline downloads, queue management, storage controls, and offline playback.
- Public GitHub Releases distribution and a protected automated release pipeline.
- Native update discovery/download/installation for stable and opt-in beta channels.
- Accessible dark-first UI, useful error states, and dependable data migrations.

### Deferred or undecided

- Website-account login/synchronization: not yet committed to version one.
- Background audio with screen off: product decision outstanding; distinct from PiP.
- Casting, Android TV, social/community/chat, watch parties, cloud sync, and trackers:
  deferred.
- External/SAF download destinations and architecture-specific APKs: deferred.
- Intro/outro skipping: only if reliable timestamp data is confirmed.

No custom backend or KMP target is needed initially. Minimum Android API 26 is a
recommendation pending dependency/device validation, not a configured build value.

## 2. Source findings and feasibility gate

During initial inspection, `https://reanime.cz/home` returned server-rendered
catalogue content and structured bootstrap objects containing source anime IDs,
titles, artwork, episode counts, and AniList references. These observations do
not establish a supported public API or permission to redistribute content.

The site-linked `https://restatus.me/` listed `.cz`, `.bz`, and `.wtf` domains,
a Re:ANIME API monitor, and Flixcloud/HD-1/HD-2 monitors. The following homepages
returned matching content during that inspection:

- `https://reanime.cz/home`
- `https://reanime.bz/home`
- `https://reanime.wtf/home`

Revalidate before implementation. This is not a permanent availability guarantee.
Do not treat analytics references to `.to` as verification of another mirror.

The footer says media is hosted by third parties. A sample watch-page HTML response
reported no streaming servers; browser-side source loading was not validated.
A direct Python request received HTTP 403 while the page-fetch tool could read
the page. Android access, API contracts, media URLs, subtitles, and downloads
remain unverified. Mirrors may share failing backend dependencies.

### Gate before full feature investment

1. Verify permitted access/terms, branding, and distribution/content rights.
2. Confirm catalogue/search/details/episodes from an emulator and physical phone.
3. Identify supported endpoints and map source identities across mirrors.
4. Play available authorized content in Media3 and verify seeking/tracks/subtitles.
5. Download representative supported content and play it with networking disabled.
6. Exercise source expiry, interruptions, low storage, and restart/resume behavior.

Record evidence and limitations. Do not bypass anti-bot, access-control, or DRM
restrictions or assume a browser-only player can be used natively.

## 3. Technology decisions

| Concern | Decision |
| --- | --- |
| Platform | Native Android, Kotlin, single-activity Compose UI |
| Presentation | Lightweight MVI; ViewModel + Coroutines + immutable StateFlow state |
| UI | Compose + Material 3 + app-owned design tokens/components |
| Navigation | AndroidX Navigation 3 |
| Requests | Ktor Client 3 + kotlinx.serialization |
| HTML | Fleeksoft Ksoup core, after fetching through Ktor |
| DI | Koin with constructor injection and explicit scopes |
| Database | AndroidX Room 3 + KSP + SQLite driver |
| Preferences | DataStore |
| Artwork | Latest compatible stable Coil 3 + Ktor 3 network integration |
| Playback/offline | AndroidX Media3 |
| Deferred jobs | WorkManager where appropriate |
| Build | Gradle Kotlin DSL, version catalog, included convention-plugin build |
| Quality | ktlint, Android Lint, Kotlin-compatible Detekt, tests |

### Recommended choices awaiting initial build verification

- Ktor's OkHttp engine for Android HTTP/2 support; app request code remains Ktor.
  If avoiding OkHttp entirely becomes a requirement, evaluate CIO and its limitations.
- BundledSQLiteDriver for consistent SQLite behavior, accepting binary-size cost.
- Ordinary Navigation 3 entry providers with Koin ViewModel injection; no requirement
  to adopt Koin's experimental Navigation 3 registration DSL.
- A stable-compatible Compose/toolchain baseline. Recheck exact versions at setup
  rather than freezing discovery-time version numbers in this document.

### Styles evaluation

Compose Styles was experimental and undergoing redesign when checked; official
documentation said it would remain experimental in Compose 1.13. Do not use the
old Style API from outdated examples or couple all feature APIs to it.

Begin with theme tokens, component parameters, and modifiers. If approved,
evaluate the current API in an anime card and a player button, considering state
styling, accessibility, previews, scroll performance, and migration burden.
Keep experimentation inside the design-system boundary and record the outcome.

## 4. Module layout and dependency direction

Start small and split only at useful ownership/build boundaries:

```text
:app                    App setup, Koin wiring, navigation, platform entry points
:core:model             App-owned catalogue, episode, progress, and error models
:core:data              Repositories, Room, DataStore, shared network configuration
:core:designsystem      Theme tokens and reusable accessible Compose components
:source:reanime         Requests, DTOs, parsing, mirrors, playback-source resolution
:feature:browse         Home/search/details/library/settings MVI initially
:feature:player         Player MVI, controls, playback/session integration
:feature:downloads      Download orchestration boundary, offline library UI/MVI
:feature:updates        Release discovery, verification, APK transfer, update UI/MVI
build-logic/            Included build for Gradle conventions
```

This is a target layout, not a requirement to create empty modules immediately.
Wire only modules required by each milestone.

- Features depend on app-owned models and repository/application APIs.
- `core:data` implements repositories using `source:reanime` and local stores.
- `source:reanime` depends on models/network libraries, never features or Room.
- App is the composition root and owns cross-feature navigation.
- Design system contains no networking, persistence, or source-specific logic.
- Keep public repository APIs distinct from their implementations; extract a
  separate API/domain module only when size/dependency pressure justifies it.
- Avoid feature-to-feature implementation dependencies; connect through app
  navigation and narrowly shared contracts.

## 5. MVI presentation contract

```text
Compose interaction -> ScreenAction -> ScreenViewModel -> repository/platform call
                                         |
                                   internal result
                                         |
                                    pure reducer
                                         |
                              StateFlow<ScreenState> -> Compose
```

Each feature owns explicit contracts, immutable state, deterministic transitions,
and operation-specific concurrency. AndroidX ViewModel is a lifecycle host, not
an alternative to MVI. No additional MVI framework is required initially.

Contract names are feature-prefixed: `BrowseScreenState`, `BrowseScreenAction`,
`BrowseScreenViewModel`, and, only when needed, `BrowseScreenEvent`. ScreenActions
are incoming UI actions; ScreenEvents are transient outgoing UI commands. This
naming replaces UiState/UiIntent/UiEffect terminology. Internal async results can
remain separate from user actions; do not generate empty ScreenEvent contracts.

- Model loading/content/error states without contradictory flags.
- Debounce/cancel superseded searches; reject stale results.
- Serialize conflicting queue/library mutations as needed.
- Keep cancellation exceptional; use typed app errors for actual failures.
- Collect state with `collectAsStateWithLifecycle()`.
- Use SavedStateHandle for small inputs and Room/DataStore for durable data.
- Reserve effects for transient actions with explicit delivery semantics. Critical
  outcomes must survive lifecycle changes as state; exactly-once is not assumed.
- Navigation/platform installation is performed at an appropriate UI/platform
  boundary. Do not launch it unpredictably from background collectors.
- Media3 owns player/download state machines. Observe progress at an appropriate
  rate; do not duplicate them as global reducers.

Test reducer transitions, races/cancellation, restoration, and effect handling.

## 6. Source and network boundaries

Suggested app-owned models include AnimeSummary, AnimeDetails, Episode,
EpisodeIdentity, PlaybackSource, SubtitleTrack, WatchProgress, and SourceError.
Keep external response details out of these contracts where possible.

### Request policy

- Explicit Ktor engine, content negotiation, timeouts, response validation,
  bounded retries, cancellation, and redacted diagnostics.
- Separate catalogue/authenticated traffic, public artwork, update discovery,
  and media-host credential contexts. Sharing a transport does not justify
  sharing authorization or domain cookies.
- Validate URLs and redirect destinations before attaching credentials.
- Fetch HTML through Ktor, parse with Ksoup core, and map to app models.
- Prefer confirmed permitted JSON APIs; do not mistake bootstrap JavaScript
  object syntax for JSON or execute arbitrary scripts.
- Parser fixture tests are deterministic; live health probes are opt-in/scheduled,
  small, permitted, and not part of ordinary pull-request checks.

### Mirrors

Begin with a bundled verified allowlist and a manual selector. Use bounded
automatic fallback for safe requests only. Persist source IDs independently of
the base URL, do not forward credentials between domains blindly, and distinguish
domain/API/media-host failure. A remotely updated signed mirror registry is deferred.

### Error taxonomy

At minimum: offline, timeout, rate limited, access blocked, remote contract changed,
missing source, expired source, unsupported format/decoder, media-host failure,
storage failure, verification failure, and installation failure/cancellation.
Preserve actionable recovery choices without exposing sensitive request data.

## 7. Persistence and identity

Room 3 stores watchlist entries, watch history/progress, cached metadata, and the
catalogue relationships needed for offline use. DataStore stores channel choice,
network/download preferences, appearance, and playback defaults.

- Use source anime and episode identities, not stream URLs, as durable keys.
- Include language/version dimensions where they distinguish episode variants.
- Keep AniList IDs as optional cross-references; no separate metadata API is
  required to browse initially.
- Export schemas, commit them, and test upgrades from shipped database versions.
- No routine destructive fallback for user data. Disposable metadata caches may
  have separate recovery policies.
- Define backup/data-extraction rules and credential exclusions explicitly.
- Plan a versioned export/import format; cloud sync is out of scope initially.

## 8. Playback ownership

Resolve playable sources near playback time and pass supported media information
to Media3. Media3 media requests use their own data-source configuration: Ktor
cookies/headers are not automatically inherited.

The player lifetime must survive supported UI transitions without being created
or released by incidental recomposition. Establish the exact owner during the
playback proof. If background audio is approved, use an appropriately configured
MediaSessionService; otherwise explicitly handle foreground/PiP lifecycle rules.

Support available tracks/subtitles, seek/speed, audio focus, headphone disconnects,
PiP, safe next-episode behavior, and progress restoration. Save progress periodically
and at important lifecycle/playback transitions without excessive DB writes.

Intro skipping requires actual timestamps. Casting and DRM support are not
implied by using Media3.

## 9. Offline download subsystem

Use Media3 DownloadManager, DownloadIndex, DownloadService, DownloadHelper where
needed, and matching offline cache/data-source configuration. WorkManager is not
the media download loop. Validate restart/scheduling behavior for supported Android
versions, including foreground-service and notification constraints.

DownloadIndex is authoritative for execution state. Room associates source/episode
metadata and library information with stable Media3 download IDs; reconcile
orphaned records rather than maintain two competing status machines.

### Version-one behavior

- Queue episodes; select available quality, audio, and subtitles before downloading.
- Pause/resume/retry/cancel and configure Wi-Fi/network requirements/concurrency.
- Show determinate progress when known and sensible indeterminate state otherwise.
- Surface storage usage, low-space errors, and explicit deletion controls.
- Retain offline media separately from disposable streaming cache.
- Store in app-specific storage without broad storage permission; clearly explain
  removal on uninstall/clear-data. Arbitrary external folders are deferred.
- Play completed content and its supported tracks with networking disabled.

Source-expiry recovery must preserve verified content identity. Do not append
fresh, potentially different media to partial content. Some sources may require
a safe restart or be unsupported. Encrypted/offline-license content requires
authorized host-specific validation; it is not automatically supported.

## 10. Native update subsystem

### Distribution and channels

Initially distribute one universal APK from a public GitHub repository. Do not
embed GitHub credentials. One application ID and APK-signing identity span stable
and beta installs. A separate debug ID avoids replacing a real installation.

- Stable is default and accepts only eligible published non-prereleases.
- Beta is opt-in and accepts eligible published beta and stable releases.
- Ignore drafts and incompatible/minimum-Android releases.
- Select by verified monotonic versionCode, not tag text or date alone.
- Stable-only `/releases/latest` is insufficient for beta discovery; filter the
  release listing and handle pagination, API errors, caching, ETags, and rate limits.
- Switching back to stable waits for a compatible newer stable build; never
  automatically downgrade or uninstall user data.

### Release assets

```text
reanime-<version>-universal.apk
update-manifest.json
update-manifest.json.sig
SHA256SUMS
```

Define the exact manifest schema and cryptographic algorithm before updater code.
It should bind schema version, application ID, channel, versionName, versionCode,
minimum Android version, asset identity/URL, byte size, SHA-256, signing-certificate
identity/policy, and release-notes reference. Sign exact published bytes, with
unambiguous detached-signature encoding. Unknown schemas/algorithms fail closed.

The app's embedded trust policy authenticates the manifest key and permitted APK
signer; the manifest must not be able to authorize an arbitrary replacement signer.
Choose a vetted signature implementation supported on the minimum Android version.
Manifest signing uses a separate key from APK signing. Document trusted-key
rotation/recovery before the first public release.

### Update flow

1. Discover eligible releases without trusting unverified metadata as instructions.
2. Verify manifest signature against an embedded trusted key.
3. Validate package, channel, versionCode, minimum OS, and asset constraints.
4. Download the APK to controlled temporary storage with progress/cancellation.
5. Check size/hash and actual APK package/version/signer against trusted policy.
6. Request installation through Android with explicit user consent.
7. Let Android enforce signature/update compatibility; clean up temporary artifacts.

Handle denied install authorization, cancellation, interrupted transfers, low storage,
GitHub outages/rate limits, and app restarts. On supported Android versions users
must authorize this installer source. No silent updates or surprise background
installation UI. Use narrow FileProvider paths or a suitable PackageInstaller session.
Do not store APK update payloads in the retained episode-download cache.

## 11. Build, CI, and release engineering

### Build foundation

Follow the build-logic structure reviewed in `Act-Aks/kswallet` at commit
`69f096d323f37f53460a8e9ca81597fea29cf758`, adapted to ReAnime rather than copied
wholesale:

- `build-logic/settings.gradle.kts` imports the root catalog and includes `:convention`.
- `build-logic/convention` contains compiled `Plugin<Project>` classes and catalog
  access/configuration helpers, not precompiled script plugins.
- Plugin implementation dependencies and registration IDs come from the catalog.
- Modules apply versionless `reanime-*` convention aliases through `libs.plugins`.
- SDK levels, JVM toolchain, application metadata, and external dependency/plugin
  versions are maintained in `gradle/libs.versions.toml`.
- Use type-safe `projects.*` accessors for module dependencies.
- Only introduce conventions actually needed; Room/network conventions land with
  their implementing modules. Do not copy template alpha dependencies or wallet logic.

Initial compatibility baseline: Gradle 9.4.1, AGP 9.2.1, Kotlin 2.3.0, JDK 21,
compile/target API 36, minimum API 26, and Compose BOM 2026.03.00. Kotlin aligns
with this Gradle version's embedded Kotlin for convention compilation. These are
explicit compatible choices, not a claim that every dependency is the newest release.
This describes the earlier verified baseline, not the current catalog. The current
baseline uses Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00,
build JDK 21, compilation toolchain 17, compile SDK 37.0, target SDK 36, and
minimum SDK 26. Runtime/toolchain JDKs are intentionally separate catalog settings.
The wrapper distribution checksum is restored and setup-gradle validates official
wrapper JAR checksums. The existing official wrapper JAR need not have the same
version as its selected distribution.

Stable Detekt 1.23.8 runs as an isolated CLI for syntax-only checks, avoiding its
older Gradle/AGP integration and compiler classpath conflicts. It does not provide
current Kotlin compiler type-resolution checks; compiler and Android Lint checks remain required.
Re-evaluate this integration when a compatible stable Detekt plugin is available.

- Gradle wrapper, Kotlin DSL, centralized repositories, version catalog, and
  compatible pinned toolchain/JDK.
- Included `build-logic` for application/library/Compose/Room/quality conventions
  as needed, without generating unused scaffolding.
- Debug, minified/resource-shrunk release, and later profileable benchmark builds.
- No product flavors until distinct products/environments justify them.
- Dependency verification and appropriate locking/update automation.
- No secrets in BuildConfig; no signing artifacts or machine-local SDK paths in git.

### Quality

`.editorconfig` is authoritative. Pin ktlint plugin/engine versions; check Kotlin,
Gradle scripts, and convention plugins while excluding generated output. Apply
formatting only through explicit `ktlintFormat`; CI runs `ktlintCheck` without edits.
Use Android Lint and a focused compatible Detekt configuration without competing
formatting rules. Fix issues instead of silently expanding baselines.

Dependency/tool upgrade advice remains visible at informational severity so an
unchanged commit does not fail merely because upstream publishes a newer version.
Correctness, API, and security warnings remain blocking. Dependabot definitions
cover reviewed Gradle/action upgrades. No warning baseline is used.

### Pull-request CI

Validate wrapper/toolchain, formatting, static analysis, all relevant JVM unit tests,
Android Lint, and debug assembly. Non-Android/build-logic tests must not be omitted
just because `:app:testDebugUnitTest` passes. Publish useful sanitized test reports.
Run instrumented tests on an appropriate separate job as they become available.

Current workflow improvements:

- PR (including title edits), `main` push, merge-group, and manual triggers.
- An independent conventions job tests the Python helpers and validates PR titles
  using an environment variable, not untrusted shell interpolation.
- A dependent Android job reads validated catalog values before Gradle setup;
  explicitly installs command-line tools/licenses and the canonical SDK package
  (`platforms;android-37.0`, not nonexistent `platforms;android-37`).
- Override `setup-android@v3`'s package list with `platform-tools`. Its default
  `tools platform-tools` requests an obsolete `tools` package and caused GitHub
  job `134816321387` to fail before Gradle. A workflow regression test guards the
  explicit override; the compile platform is still installed in the next step.
- Both JDKs are installed; the build runtime is last/active, and Gradle receives
  the toolchain environment-variable names for deterministic discovery.
- Checksum-pinned wrapper distribution, wrapper-JAR validation, major-version action tags,
  read-only contents permission, and cache writes restricted to `main` pushes.
- `verifyFoundation` checks every current Android module's debug unit-test task,
  plus existing formatting/Detekt/lint/debug assembly and included-build checks.
- `verifyRelease` checks full release lint and unsigned minified/resource-shrunk
  assembly. It does not publish or access signing secrets.
- Failure reports are retained for seven days unless cancelled; only successful
  `main` pushes upload a debug APK, not PR or unsigned release APKs.
- Dependabot uses Conventional Commit prefixes so its PR titles pass the same
  validation. The final `CI` job requires both conventions and Android verification
  to succeed, preventing a skipped dependent job from hiding an earlier failure.
  Configure that stable `CI` check as required in GitHub after it first runs.
- Add new module report/test tasks when modules land. Device/emulator tests remain
  pending, and required checks/merge settings must be configured by a repo admin.

### Protected release pipeline

1. Select/validate a version tag and trusted source revision.
2. Allocate/check a globally increasing versionCode across stable/beta/hotfixes.
3. Run release validation/tests and build the release APK.
4. Sign and verify the APK; generate hash and detached signed manifest.
5. Upload all assets/release notes to a draft release.
6. Verify published asset bytes/metadata and completeness.
7. Publish as stable release or beta prerelease only after validation.

Use protected environments/approvals, serialized release execution, minimal
GITHUB_TOKEN permissions, readable major-version action tags, and explicit trusted-ref policy.
Major-version tags are mutable rather than immutable SHA pins; this is the owner's
explicit preference. Retain artifact checksums and review Dependabot major updates.
Do not use PR-controlled workflows/code in a secrets-bearing context. Avoid
unnecessary long-lived personal access tokens and do not log secret material.

Version names follow SemVer-like labels, e.g. `1.0.0-beta.1`, `1.0.0`,
`1.1.0-beta.1`. The allocator mechanism must be selected and documented in the
build milestone; dates/tag strings alone are not sufficient. Promotion semantics
must distinguish publishing the identical artifact from building a newer stable
artifact; never assign different bytes the same package/versionCode.

Back up APK/manifest signing keys independently of GitHub. Losing APK-signing
identity can strand installations. A bad release is repaired by a higher-version
release, not a forced downgrade. Document revocation/withdrawal and key-rotation policy.

## 12. Verification and nonfunctional requirements

- Unit: MVI reducers/concurrency, repositories, Ktor MockEngine, parser fixtures,
  error mappings, update selection/signature/hash validation, Koin wiring.
- Persistence: schema/migration tests and restoration of real shipped data shapes.
- UI: entry-scoped ViewModels, independent detail entries, tabs, predictive back,
  loading/errors, permission/installer cancellation, accessibility/font scaling.
- Device: playback formats/tracks, PiP, rotation, interruptions, process death,
  actual offline playback, download restart, network changes, and low storage.
- Upgrade: signed beta-to-stable progression, channel switching, tampered manifests/
  APKs, wrong signers/packages, interrupted transfers, preserved DB/offline media.
- Performance: startup/scroll baselines, sized artwork, lazy-list keys, sampled
  progress, Baseline Profiles/Macrobenchmark once real flows exist.
- Privacy: minimal diagnostics; no secrets/signed URLs in logs; user-controlled
  sharing; explicit cache/history/library deletion behavior.

## 13. Implementation milestones

1. **Governance:** establish these documents and preserve confirmed/open decisions.
2. **Foundation:** scaffold minimal modules, toolchain, MVI contracts, DI, ktlint,
   static analysis, initial tests, and PR CI. Document runnable verification tasks.
3. **Feasibility:** permitted source access -> catalogue -> native playback ->
   offline download proof, including mirror/expiry behavior on physical Android.
4. **Browse/library:** home, search, details, episodes, Room persistence, settings.
5. **Playback:** polished controls, tracks/subtitles, resume, media/PiP lifecycle.
6. **Offline:** queue UI, network/storage policies, resilience, offline verification.
7. **Updates/releases:** finalized signed manifest, updater, protected signing/publishing,
   and beta-to-stable end-to-end installation tests.
8. **Hardening:** accessibility, performance, migration/release checks, public beta,
   then stable release.

Do not interpret the milestone list as existing functionality. Update status with
actual evidence as implementation proceeds.

### Current implementation evidence

- Modules present: `:app`, `:core:designsystem`, `:feature:browse`, plus the
  `build-logic` included build's `:convention` plugin module.
- Native placeholder screen, Navigation 3 home/about entries, Koin wiring, initial
  dark theme, and ScreenState/ScreenAction/ScreenViewModel MVI example.
- Five browse reducer/ViewModel tests pass, including saved-input re-creation
  (not a full Android process-death instrumentation test).
- The earlier documented compatibility baseline passed `./gradlew verifyFoundation`
  locally, including convention checks, formatting, syntax-only Detekt, unit tests,
  Android Lint, and debug assembly.
- During initial Git setup, the upgraded snapshot (Gradle 9.9.1, AGP 9.5.1, Kotlin
  2.4.21, Compose BOM 2026.09.00, Lifecycle 2.11.0, JVM 17) failed AAR metadata checks:
  Compose/Lifecycle require compile SDK 37+, but the catalog still specifies 36.
  These existing configuration changes were preserved at initialization. The CI
  refinement raises compile SDK to 37.0 without changing target SDK or reverting
  the user's dependency upgrades.
- GitHub run `44305245950` failed before Gradle because `sdkmanager` was absent from
  PATH. Explicit SDK-tools setup fixes that workflow gap; changes need a push and
  a new hosted run to establish GitHub success.
- Final local verification passed `./gradlew verifyFoundation verifyRelease
  --console=plain --continue`, including all five browse tests, debug/release lint,
  debug assembly, R8, and release resource shrinking with compile SDK 37.0.
- All 12 CI-helper test methods passed. Actionlint 1.7.12 validated workflow syntax,
  expressions, and action inputs. ShellCheck was unavailable and its integration
  was not run. These local checks do not claim a successful hosted SDK installation
  or Linux build; the workflow still needs a new GitHub run after push.
- No release workflow, release signing, or updater is implemented.
- No emulator/physical-device, source playback, download, database, or upgrade
  compatibility results are claimed yet.

## 14. Decisions still required

- Branding permissions (display name and application ID are confirmed above).
- Release approvers, signing-key custody, and repository protection settings
  (GitHub repository and Ubuntu CI runner are now configured in source).
- Final compatible toolchain/dependency versions and minimum Android support.
- Confirmed permitted catalogue/playback/download contracts and supported hosts.
- Background audio and website-account support scope.
- Exact versionCode allocation, manifest schema/signature algorithm, and key-rotation policy.
- Download default quality, concurrency/storage limits, and update-check preferences.

Do not invent repository coordinates, credentials, signing keys, or verified source
capabilities to fill these gaps. Use explicit placeholders only in nonfunctional
examples; ask for decisions when implementation depends on them.
