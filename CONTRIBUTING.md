# Contributing

Lifes is developed primarily for the Zyle Extreme permadeath mode, but clean patches are welcome. Read [README.md](README.md) first: this project targets Paper **1.21.11** and will not be maintained past the point where the mode is fully shipped. Contribute knowing the destination.

## Opening up

**Contributions and forks are open while the repository is in its working period**, that is, until the mode ships and maintenance winds down. After that, the repository becomes a finished artifact: forks are the way forward, pull requests will slow to a stop.

## Ground rules

- Conventional Commits for every commit (`feat:`, `fix:`, `docs:`, ...). Pull request titles are validated in CI.
- Java 21, UTF-8, 4-space indentation (see `.editorconfig`).
- Every class stays small and single-purpose; no `Manager` grab-bags.
- Public API lives under `it.hydr4.lifes.api` and follows SemVer; everything else may move between minor versions.
- New behavior ships with tests. Pure-domain logic needs no server; use the Democracy testkit for command tests.
- Changelog entries follow [Keep a Changelog](https://keepachangelog.com/): add yours under the `Unreleased` heading.

## Using AI agents on contributions

AI use must be **thoughtful and targeted, absolutely not fully autonomous**:

1. Give an agent a bounded task with a clear definition of done.
2. Review its output line by line; run the full build.
3. Before opening a PR, make sure you can explain **exactly** what the agent's workflow was: what it worked on, how it worked, and why every change it produced is correct. If you cannot, do not open the PR yet, because you are the one signing off.
4. Note the AI involvement briefly in the PR description.

Bulk-dumped, unreviewed agent output will be rejected.

## Building and testing

```bash
./gradlew clean build
```

`check` includes SemVer validation of the build version and the full test suite. The architecture boundary test (`SourceBoundaryTest`) will fail if Bukkit leaks into the domain packages.

## Pull requests

1. Branch from `main`.
2. Keep the change focused; one concern per pull request.
3. Run the full build before pushing.
4. Describe the user-visible behavior change, and the agent workflow if AI was involved.
