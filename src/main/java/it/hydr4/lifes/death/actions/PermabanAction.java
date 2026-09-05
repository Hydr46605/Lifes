package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import it.hydr4.lifes.text.TemplateRenderer;
import net.kyori.adventure.text.Component;

import java.time.Instant;

/** Permanently bans the affected player and kicks them if they are online. */
public final class PermabanAction implements LifesAction {
    private final String reasonTemplate;

    private PermabanAction(String reasonTemplate) {
        this.reasonTemplate = reasonTemplate;
    }

    public static PermabanAction from(DeathActionSpec spec) {
        return new PermabanAction(spec.optionalString("reason", "You have run out of lives."));
    }

    @Override
    public void execute(ActionContext context) {
        var change = context.change();
        var offline = context.player();
        var profile = offline.getPlayerProfile();
        var reason = TemplateRenderer.renderPlain(reasonTemplate, TemplateRenderer.pairs(
            "player", profile.getName(),
            "uuid", profile.getId().toString(),
            "lives", change.after().lives()
        ));
        if (!offline.isBanned()) {
            offline.ban(reason, (Instant) null, "Lifes");
        }
        var player = offline.getPlayer();
        if (player != null) {
            player.kick(Component.text(reason));
        }
    }
}
