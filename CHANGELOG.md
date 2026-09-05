# Changelog

All notable changes to Lifes are documented in this file. The format follows [Keep a Changelog](https://keepachangelog.com/) and the project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.1.2] - 2026-09-05

### Fixed

- Tab completion was still dead after 0.1.1. The provider had been repaired, but the tree it hung from could not work: everything after `/lives` was parsed as one greedy string argument, so the client was told to replace the rest of the line rather than the word being typed. Accepting a name out of `/lives check ` produced `/lives Ghost` and dropped the subcommand, and `/lives ` had no subcommand nodes to offer at all. `/lives` is now registered as a native Brigadier tree built from the generated descriptor, with one node per subcommand and one per argument, typed as a word and an integer. Completion replaces only the current word, and a non-numeric amount is refused by the parser with the vanilla error instead of reaching the handler.
- A suggestion provider that threw produced an empty list with no trace of the failure. Provider errors are now logged against the provider key.

### Changed

- `vite` and `lifes` are registered as aliases of a single tree instead of three roots sharing one.
- Each admin subcommand now carries its own permission requirement, so a sender without it does not receive the node in their command tree at all. The bare `/lives` deliberately carries none, because gating the root would hide every subcommand, and answers with the new configurable `no-permission` message when the sender may not run it.

## [0.1.1] - 2026-09-05

### Fixed

- Tab completion for player arguments returned nothing at all. The provider read the Bukkit API on Paper's suggestion threads, where that is not allowed, and swallowed the resulting failure into an empty list. Suggestions now come from a name index kept in step on the main thread, and known offline accounts are offered too.
- `/lives add <player> <huge amount>` overflowed to a negative number, clamped the result to zero lives and ran the exit pipeline, so a command meant to give lives permanently banned the target. The sum is now computed without wrapping.
- An account left at zero lives could play forever after its ban was lifted, because the exit pipeline only fired on the transition into zero. `exhaustion.on-zero-lives-join` now decides what a connect does: `REAPPLY` (default), `KICK` or `IGNORE`. A death that finds an account already at zero lives re-runs the pipeline as well, which also covers a crash between the save and the ban.
- A single unreadable entry in `saves.yml` was skipped in silence and then erased for good by the next full save. Any damaged entry now preserves a copy of the file and aborts startup, the same way root-level corruption already did.
- `saves.yml` with a missing or non-integer `version` loaded as version 1; it now aborts like any other unsupported file.
- The PlaceholderAPI expansion reported a hardcoded `0.1.0` no matter which build was running. It now publishes the version the jar was built with.

### Changed

- The default `lives-self` and `lives-other` messages no longer render `{lives}/{maximum}`. `lives.maximum` is the cap on admin commands, not the player's life pool, so the fraction read as a false total. The `{maximum}` placeholder still resolves for anyone who wants it in their own template.

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

[Unreleased]: https://github.com/Hydr46605/Lifes/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/Hydr46605/Lifes/releases/tag/v0.1.1
[0.1.0]: https://github.com/Hydr46605/Lifes/releases/tag/v0.1.0
