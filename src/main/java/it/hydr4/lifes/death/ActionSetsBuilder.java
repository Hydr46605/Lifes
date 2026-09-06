package it.hydr4.lifes.death;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.death.actions.CommandAction;
import it.hydr4.lifes.death.actions.DiscordAction;
import it.hydr4.lifes.death.actions.MessageAction;
import it.hydr4.lifes.death.actions.PermabanAction;
import it.hydr4.lifes.death.actions.SoundAction;
import it.hydr4.lifes.discord.DiscordGateway;

import java.util.ArrayList;
import java.util.List;

/** Converts settings action specs into typed actions, failing on unknown types. */
public final class ActionSetsBuilder {
    private ActionSetsBuilder() {
    }

    /**
     * Builds both pipelines. The gateway is passed in rather than created here: it owns a worker
     * thread and must survive reloads, so the plugin owns its lifetime.
     */
    public static ActionSets build(LivesSettings settings, DiscordGateway gateway) {
        return new ActionSets(
            convert(settings.deathActions(), gateway),
            convert(settings.exhaustionActions(), gateway)
        );
    }

    private static List<LifesAction> convert(List<DeathActionSpec> specs, DiscordGateway gateway) {
        var actions = new ArrayList<LifesAction>(specs.size());
        for (var spec : specs) {
            actions.add(create(spec, gateway));
        }
        return List.copyOf(actions);
    }

    private static LifesAction create(DeathActionSpec spec, DiscordGateway gateway) {
        try {
            return switch (spec.type()) {
                case "MESSAGE" -> MessageAction.from(spec);
                case "SOUND" -> SoundAction.from(spec);
                case "COMMAND" -> CommandAction.from(spec);
                case "PERMABAN" -> PermabanAction.from(spec);
                case "DISCORD" -> DiscordAction.from(spec, gateway);
                default -> throw new IllegalArgumentException(
                    "unknown action type '" + spec.type()
                        + "'; expected MESSAGE, SOUND, COMMAND, PERMABAN or DISCORD"
                );
            };
        } catch (IllegalArgumentException exception) {
            throw new ConfigException(spec.path(), exception.getMessage());
        }
    }
}
