# Changelog

All notable changes to Lifes are documented in this file. The format follows [Keep a Changelog](https://keepachangelog.com/) and the project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-09-05

### Added

- Permadeath lives core: tracked lives per account, configurable default and maximum, death cost, ignored death causes.
- Death and exhaustion pipelines with ordered, typed actions: `MESSAGE`, `SOUND`, `COMMAND`, `PERMABAN`.
- Command suite `/lives` (aliases `vite`, `lifes`): self check, `check`, `set`, `add`, `remove`, `reset`, `reload`, with permission-filtered tab completion.
- Typed `settings.yml` with strict validation: unknown keys, wrong types and out-of-range values fail with the exact configuration path.
- Versioned `saves.yml` with atomic writes, off-thread saving, periodic flush, corrupt-file preservation (`saves.yml.broken-<timestamp>`) and per-entry salvage.
- PlaceholderAPI expansion: `lives`, `max`, `default`, `remaining`, `total_deaths`, `status`, `last_death`.
- Bukkit `LifeChangeEvent` for integrations; domain listeners for internal reactions.
- Hermetic build: checksum-pinned Gradle, `-Werror`, SemVer validation, CI on Linux and Windows, release automation for `v` tags.

[Unreleased]: https://github.com/Hydr46605/Lifes/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Hydr46605/Lifes/releases/tag/v0.1.0
