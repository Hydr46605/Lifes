<p align="center">
  <img src="assets/lifes-logo.png" width="220" alt="Lifes logo">
</p>

# Lifes

Permadeath lives for PaperMC servers. Every player owns a number of lives, each death spends one, and when the last life is gone the configured exit pipeline runs. The default pipeline is a permanent ban.

Built for **Zyle Extreme**, the permadeath mode of the Minecraft server **Zyle**. The mode is still in development and has not been released yet, but the door is open: come take a look on [Discord](https://discord.gg/99cYsbSxmR).

| | |
|---|---|
| Platform | Paper 1.21.11 |
| Java | 21 |
| Release | [v0.4.1](https://github.com/Hydr46605/Lifes/releases/tag/v0.4.1) |
| License | [Apache 2.0](LICENSE) |

## Install

Drop the jar into `plugins/`, start the server once, edit `plugins/Lifes/settings.yml`, then run `/lives reload`.

## Commands

Aliases: `vite`, `lifes`.

| Command | Permission (default) | What it does |
|---|---|---|
| `/lives` | `lifes.command.check.self` (everyone) | Shows your lives |
| `/lives check <player>` | `lifes.command.check.others` (op) | Shows another account |
| `/lives set <player> <amount>` | `lifes.command.set` (op) | Sets lives, rejected outside `0..maximum` |
| `/lives add <player> <amount>` | `lifes.command.add` (op) | Gives lives |
| `/lives remove <player> <amount>` | `lifes.command.remove` (op) | Removes lives |
| `/lives reset <player>` | `lifes.command.reset` (op) | Restores the default amount |
| `/lives list dead` | `lifes.command.list` (op) | Lists exhausted accounts, A-Z, first 20 |
| `/lives reload` | `lifes.command.reload` (op) | Reloads `settings.yml` |

Admin operations work on offline players whenever the account is known to Lifes. Tab completion only offers what the sender is allowed to run, and lists online players before known offline accounts.

## Placeholders

Requires PlaceholderAPI. All of them resolve for offline players with a known account.

| Placeholder | Value |
|---|---|
| `%lifes_lives%` | Current lives |
| `%lifes_max%` | Configured maximum |
| `%lifes_default%` | Configured default |
| `%lifes_remaining%` | Maximum minus current lives |
| `%lifes_total_deaths%` | Recorded deaths |
| `%lifes_status%` | `alive` or `exhausted` |
| `%lifes_last_death%` | ISO-8601 instant, or `never` |
| `%lifes_dead_count%` | Number of exhausted accounts |
| `%lifes_dead_list%` | Dead names A-Z, first 20, ` (+N more)` when truncated |
| `%lifes_dead_list_<limit>_<sep>%` | Same, up to 100 names; `sep` is `comma`, `newline`, `pipe` or `plus` (`:` works like `_`) |
| `%lifes_last_victim%` | Last player hit by a counted death (empty until the first one) — pair with `PLAYER_HEAD:%lifes_last_victim%` for victim heads |

## Configuration

`settings.yml` documents every option inline. The rules worth knowing:

- Unknown keys, wrong types and out-of-range values fail startup with the exact configuration path. There are no silent fallbacks.
- `death.ignored-causes` skips deaths matching an `EntityDamageEvent.DamageCause` name.
- `death.actions` runs on every death that costs a life. `exhaustion.actions` runs when lives reach zero, and `PERMABAN` is the default exit there. `gain.actions` runs on any life gain that does not leave zero lives, `resurrect.actions` only when a gain leaves zero (`before.lives == 0`, `delta > 0`). All action templates share `{player}` `{lives}` `{delta}` `{before}` `{reason}` (plus `{uuid}` `{maximum}`, and `{deaths}` on Discord).
- `exhaustion.on-zero-lives-join` covers an account that connects while already at zero lives, which is what happens after an admin lifts a ban: `REAPPLY` runs the exit pipeline again, `KICK` only removes the session, `IGNORE` lets them play on.
- Actions are ordered and typed: `MESSAGE`, `SOUND`, `COMMAND`, `PERMABAN`, `DISCORD`, `ULTIMATEUI_OPEN`, `ULTIMATEUI_CLOSE`, `ULTIMATEUI_SET` (the last three need UltimateUI attached and fail startup with their path otherwise).
- `ultimateui.refresh-gui` + `refresh-element` + `refresh-text` rewrite one HUD text element after every life change while that GUI is open; both blank disables it.
- `DISCORD` posts a raw Discord JSON payload to a webhook, so deaths and eliminations can land in different channels. The payload is validated at load, placeholders are JSON-escaped, and delivery retries rate limits without ever stalling the server.
- `saves.yml` is written atomically and off the main thread. Any damage, at the root or inside a single entry, is preserved as `saves.yml.broken-<timestamp>` and aborts startup instead of dropping accounts or resetting data.
- Optional hooks, all no-ops when the target plugin is absent: Skript (the running `LivesService` is published to the Bukkit `ServicesManager` and to `it.hydr4.lifes.hook.LifesSkript` for skript-reflect, `LifeChangeEvent` fires as a Bukkit event) and UltimateUI (reflection-only hook gated on the investigated build's API shape, refuses on mismatch).

## Skript

Requires Skript + skript-reflect. One import per script file:

```
import:
    it.hydr4.lifes.hook.LifesSkript
```

```applescript
set {_lives} to LifesSkript.livesOfName("Hydr4").orElse(-1)
set {_dead::*} to LifesSkript.deadNames(10)
LifesSkript.adjust(player's uuid, "ADMIN_ADD", 1)
# Any placeholder without PlaceholderAPI:
set {_line} to LifesSkript.placeholder("dead_list_5_newline", null)
# UltimateUI (false when the player is offline):
LifesSkript.setElement(player's uuid, "lives-text", "3 lives left")
```

Life changes arrive as a regular Bukkit event (`it.hydr4.lifes.paper.LifeChangeEvent`)
with `getChange()` exposing `before()`, `after()`, `delta()` and `reason()`.

## Building

```bash
./gradlew clean build
```

The build is hermetic: checksum-pinned Gradle distribution, UTF-8, `-Werror`, and a SemVer validation task wired into `check`. The version comes from `lifesVersion` in `gradle.properties`, or from `LIFES_VERSION` in the environment.

## Project status

Lifes targets Paper **1.21.11** because that is what Zyle runs, and it is a finished tool for one job rather than a general-purpose library. Once the mode is fully shipped, active maintenance stops: no roadmap for newer Minecraft versions, no long-term support branch. Forks are explicitly welcome, and that is the point of the license.

## Contributing

Contributions and forks are open while the repository is in its working period. Read [CONTRIBUTING.md](CONTRIBUTING.md) for the ground rules, [SECURITY.md](SECURITY.md) to report a vulnerability, and [CHANGELOG.md](CHANGELOG.md) for what changed. The changelog follows [Keep a Changelog](https://keepachangelog.com/) and the project follows [Semantic Versioning](https://semver.org/).

---

> [!NOTE]
> **Built with AI, shipped by a human.** Lifes was developed with the help of AI coding tools under Hydr4's direction. Architecture, decisions, review and testing are human; the boring parts were accelerated. See [AGENTS.md](AGENTS.md) for the rules this repo uses when AI is involved.
