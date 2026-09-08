package it.hydr4.lifes.death;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.death.actions.CommandAction;
import it.hydr4.lifes.death.actions.DiscordAction;
import it.hydr4.lifes.death.actions.MessageAction;
import it.hydr4.lifes.death.actions.PermabanAction;
import it.hydr4.lifes.death.actions.SoundAction;
import it.hydr4.lifes.death.actions.UltimateUiCloseAction;
import it.hydr4.lifes.death.actions.UltimateUiOpenAction;
import it.hydr4.lifes.death.actions.UltimateUiSetAction;
import it.hydr4.lifes.discord.DiscordGateway;
import it.hydr4.lifes.hook.UltimateUiHook;

import java.util.ArrayList;
import java.util.List;

/** Converts settings action specs into typed actions, failing on unknown types. */
public final class ActionSetsBuilder {
    private ActionSetsBuilder() {
    }

    /**
     * Builds all four pipelines. The gateway is passed in rather than created here: it owns a worker
     * thread and must survive reloads, so the plugin owns its lifetime. The UltimateUI hook may be
     * null when the plugin is absent; configs using ULTIMATEUI_* actions then fail with their path.
     */
    public static ActionSets build(LivesSettings settings, DiscordGateway gateway) {
        return build(settings, gateway, null);
    }

    public static ActionSets build(LivesSettings settings, DiscordGateway gateway, UltimateUiHook hook) {
        return new ActionSets(
            convert(settings.deathActions(), gateway, hook),
            convert(settings.exhaustionActions(), gateway, hook),
            convert(settings.gainActions(), gateway, hook),
            convert(settings.resurrectActions(), gateway, hook)
        );
    }

    private static List<LifesAction> convert(List<DeathActionSpec> specs, DiscordGateway gateway, UltimateUiHook hook) {
        var actions = new ArrayList<LifesAction>(specs.size());
        for (var spec : specs) {
            actions.add(create(spec, gateway, hook));
        }
        return List.copyOf(actions);
    }

    private static LifesAction create(DeathActionSpec spec, DiscordGateway gateway, UltimateUiHook hook) {
        try {
            return switch (spec.type()) {
                case "MESSAGE" -> MessageAction.from(spec);
                case "SOUND" -> SoundAction.from(spec);
                case "COMMAND" -> CommandAction.from(spec);
                case "PERMABAN" -> PermabanAction.from(spec);
                case "DISCORD" -> DiscordAction.from(spec, gateway);
                case "ULTIMATEUI_OPEN" -> UltimateUiOpenAction.from(spec, hook);
                case "ULTIMATEUI_CLOSE" -> UltimateUiCloseAction.from(spec, hook);
                case "ULTIMATEUI_SET" -> UltimateUiSetAction.from(spec, hook);
                default -> throw new IllegalArgumentException(
                    "unknown action type '" + spec.type()
                        + "'; expected MESSAGE, SOUND, COMMAND, PERMABAN, DISCORD,"
                        + " ULTIMATEUI_OPEN, ULTIMATEUI_CLOSE or ULTIMATEUI_SET"
                );
            };
        } catch (IllegalArgumentException exception) {
            throw new ConfigException(spec.path(), exception.getMessage());
        }
    }
}
