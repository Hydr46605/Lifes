# AGENTS.md

Notes for AI coding agents (and for humans driving them) working on Lifes.

## What this project is

A permadeath lives plugin for **Paper 1.21.11**, built primarily for the Zyle Extreme mode of the Zyle Minecraft server. The mode is in development and not released yet. It is a focused, near-finished product, not a playground. Treat every change as something that will run on a production permadeath server where a bug can wrongly ban a player or corrupt save data.

## Non-negotiable rules

- **Paper 1.21.11 is the target.** Do not bump `paper-api` to anything else, do not "modernize" to newer Minecraft lines.
- **The domain stays platform-free.** `api`, `core`, `config`, `persistence` and `text` must not reference Bukkit/Paper (MiniMessage in `text` is the one exception, `me.clip` is banned there too). `SourceBoundaryTest` enforces this.
- **Strict config is a feature.** Never add silent fallbacks, default-on-error behavior, or "repairs" to `SettingsParser` or the persistence layer. Invalid input must fail with the exact configuration path.
- **Small classes, one job each.** No `Manager` grab-bags, no utility dumping grounds.
- **Conventional Commits, SemVer, Keep a Changelog.** The changelog gets an entry for every user-visible change.

## Build and test

```bash
./gradlew clean build
```

`check` runs the full JUnit suite, the architecture boundary test and SemVer validation. Tests are plain JUnit; the domain does not need a server. Use the Democracy testkit (`CommandTestHarness`) for command-level tests.

## AI-assisted contributions

AI agents are welcome as tools, under these conditions:

- **Targeted, not autonomous.** Use an agent for a bounded task: write this parser test, port this action, find why this edge fails. Do not hand it the repository and walk away, and do not ship fully agent-generated changes.
- **You own every line you open a PR for.** Before opening a pull request you must be able to explain, in your own words: the exact workflow the agent went through, what it worked on, how it did it, and why each resulting change is correct. If you cannot, the PR is not ready.
- **Say where AI was involved.** A short note in the PR description ("implemented with an AI agent, reviewed and tested by me") is enough. Undisclosed bulk-generated PRs get closed.
- **The usual bar still applies.** Tests for new behavior, changelog entry, no new dependencies without discussion, no breaking `it.hydr4.lifes.api` changes in `0.x` minor versions.

## Repo conventions

- Default branch: `main`. Tags: annotated, `v`-prefixed (`v0.1.0`).
- Releases: pushing a `v*` tag builds and publishes a GitHub release with SHA-256 checksums; the tag must match `lifesVersion` in `gradle.properties`.
- Gradle wrapper is a bootstrap script (`gradle/wrapper/gradle-bootstrap.ps1`) with a pinned, checksum-verified distribution; there is no wrapper jar in git by design.
- Democracy is vendored as jars under `vendor/democracy/` with recorded provenance; regenerate them from its tag, never hand-edit them.
