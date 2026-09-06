package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import it.hydr4.lifes.text.TemplateRenderer;
import org.bukkit.Bukkit;

/** Dispatches a console command with substituted placeholders. */
public final class CommandAction implements LifesAction {
    private final String template;

    private CommandAction(String template) {
        this.template = template;
    }

    public static CommandAction from(DeathActionSpec spec) {
        spec.requireKeys("command");
        return new CommandAction(spec.requiredString("command"));
    }

    @Override
    public void execute(ActionContext context) {
        var change = context.change();
        var command = TemplateRenderer.renderPlain(template, TemplateRenderer.pairs(
            "player", change.after().name(),
            "uuid", change.after().uuid().toString(),
            "lives", change.after().lives(),
            "maximum", context.maximumLives()
        ));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
