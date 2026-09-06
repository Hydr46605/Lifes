package it.hydr4.lifes;

import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.config.SettingsParser;
import it.hydr4.lifes.death.ActionSets;
import it.hydr4.lifes.death.ActionSetsBuilder;
import it.hydr4.lifes.discord.DiscordGateway;
import it.hydr4.lifes.text.Messages;
import org.bukkit.event.entity.EntityDamageEvent;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Reloadable state; reloads swap settings, messages and actions atomically. */
public final class LifesRuntime {
    private final Path settingsFile;
    private final DiscordGateway discord;
    private final AtomicReference<Snapshot> snapshot;

    private record Snapshot(LivesSettings settings, Messages messages, ActionSets actions) {
    }

    private LifesRuntime(Path settingsFile, DiscordGateway discord, Snapshot initial) {
        this.settingsFile = settingsFile;
        this.discord = discord;
        this.snapshot = new AtomicReference<>(initial);
    }

    /**
     * Loads and fully validates the runtime from disk.
     *
     * <p>The gateway is supplied rather than built here: it owns a worker thread, and a reload must
     * re-read configuration without restarting delivery mid-flight.
     */
    public static LifesRuntime load(Path settingsFile, DiscordGateway discord) {
        return new LifesRuntime(settingsFile, discord, buildSnapshot(settingsFile, discord));
    }

    /** Re-parses, validates and atomically swaps the runtime. */
    public void reload() {
        snapshot.set(buildSnapshot(settingsFile, discord));
    }

    public LivesSettings settings() {
        return snapshot.get().settings();
    }

    public Messages messages() {
        return snapshot.get().messages();
    }

    public ActionSets actions() {
        return snapshot.get().actions();
    }

    public int maximumLives() {
        return settings().maximumLives();
    }

    private static Snapshot buildSnapshot(Path settingsFile, DiscordGateway discord) {
        var settings = SettingsParser.parse(settingsFile);
        validateCauses(settings);
        return new Snapshot(
            settings,
            new Messages(settings.messages()),
            ActionSetsBuilder.build(settings, discord)
        );
    }

    private static void validateCauses(LivesSettings settings) {
        var valid = Arrays.stream(EntityDamageEvent.DamageCause.values()).map(Enum::name).toList();
        for (var name : settings.ignoredDeathCauses()) {
            if (!valid.contains(name)) {
                throw new ConfigException("death.ignored-causes", "unknown damage cause '" + name + "'; valid names: " + valid);
            }
        }
    }
}
