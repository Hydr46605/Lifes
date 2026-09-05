package it.hydr4.lifes.death;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.death.actions.CommandAction;
import it.hydr4.lifes.death.actions.MessageAction;
import it.hydr4.lifes.death.actions.PermabanAction;
import it.hydr4.lifes.death.actions.SoundAction;

import java.util.ArrayList;
import java.util.List;

/** Converts settings action specs into typed actions, failing on unknown types. */
public final class ActionSetsBuilder {
    private ActionSetsBuilder() {
    }

    public static ActionSets build(LivesSettings settings) {
        return new ActionSets(
            convert(settings.deathActions()),
            convert(settings.exhaustionActions())
        );
    }

    private static List<LifesAction> convert(List<DeathActionSpec> specs) {
        var actions = new ArrayList<LifesAction>(specs.size());
        for (var spec : specs) {
            actions.add(create(spec));
        }
        return List.copyOf(actions);
    }

    private static LifesAction create(DeathActionSpec spec) {
        try {
            return switch (spec.type()) {
                case "MESSAGE" -> MessageAction.from(spec);
                case "SOUND" -> SoundAction.from(spec);
                case "COMMAND" -> CommandAction.from(spec);
                case "PERMABAN" -> PermabanAction.from(spec);
                default -> throw new IllegalArgumentException(
                    "unknown action type '" + spec.type() + "'; expected MESSAGE, SOUND, COMMAND or PERMABAN"
                );
            };
        } catch (IllegalArgumentException exception) {
            throw new ConfigException(spec.path(), exception.getMessage());
        }
    }
}
