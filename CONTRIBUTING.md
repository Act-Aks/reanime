# Contributing to ReAnime

Read [AGENTS.md](AGENTS.md) and [the architecture](docs/ARCHITECTURE.md) before
making changes. Build configuration is maintained through the shared version
catalog and compiled conventions described in [build-logic/README.md](build-logic/README.md).

## Branches and commits

`main` is the integration branch. Use short-lived branches such as
`feat/offline-downloads`, `fix/playback-resume`, or `chore/build-tooling`.

Follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):

```text
type(optional-scope): imperative summary

Optional body explaining why the change is needed.

Optional trailers, including BREAKING CHANGE when applicable.
```

Examples:

```text
feat(downloads): add episode queue controls
fix(player): restore the selected subtitle track
build(conventions): centralize Android SDK settings
ci(release): verify signed assets before publishing
docs(architecture): clarify playback ownership
chore: initialize ReAnime Android foundation
```

Use `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`,
`style`, or `revert` as appropriate. Mark an incompatible public behavior/API
change with `!` after the type/scope and explain it in a `BREAKING CHANGE:` footer.
Do not classify an incomplete placeholder as a working feature.

Keep commits focused and buildable where practical. Do not include generated
outputs, local SDK/IDE configuration, credentials, signing keys, or unrelated work.
Stage deliberately and inspect `git diff --cached` before committing.

## Pull requests and future release automation

Use a Conventional Commit subject as the PR title. Once GitHub is configured,
prefer **squash merging with the PR title as the final commit subject**. That gives
future release tooling a consistent history even when work-in-progress commits
were used on a branch. These GitHub merge/protection settings are not configured
by local Git initialization.

Under the intended semantic-release mapping, `fix`/`perf` indicate patch changes,
`feat` indicates minor changes, and breaking changes indicate major changes.
Other types are normally maintenance-only unless an explicit release rule says
otherwise. This is a convention for the future release integration, **not an
already installed automatic versioning/publishing system**.

Intended release tags:

- Stable: `v1.0.0`
- Beta: `v1.0.0-beta.1`

Only the protected release process should create release tags after validating
version metadata and signed artifacts. Do not tag this initial development
snapshot as a public release. Android `versionCode` allocation remains a separate,
globally monotonic requirement across stable, beta, and hotfixes; commit types,
SemVer strings, and Git commit counts are not substitutes.

No remote repository, release workflow, signing secrets, or protected branch
settings exist yet. Connecting GitHub must include CI checks, squash-title
validation, trusted publishing refs, and signing/update-manifest verification.

## Verification

Format explicitly, then run verification as a separate command:

```sh
./gradlew ktlintFormat :build-logic:ktlintFormat :build-logic:convention:ktlintFormat
./gradlew verifyFoundation
```

Document results and limitations honestly. A successful build does not establish
working reAnime playback, offline access, or signed-update compatibility.
