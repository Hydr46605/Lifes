package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import it.hydr4.lifes.text.TemplateRenderer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Sends a rendered MiniMessage message to the victim or to the whole server. */
public final class MessageAction implements LifesAction {
    private final Target target;
    private final String template;

    private MessageAction(Target target, String template) {
        this.target = target;
        this.template = template;
    }

    public static MessageAction from(DeathActionSpec spec) {
        var target = Target.valueOf(spec.optionalChoice("target", Target.VICTIM.name(), Target.names()).toUpperCase(Locale.ROOT));
        return new MessageAction(target, spec.requiredString("message"));
    }

    @Override
    public void execute(ActionContext context) {
        var change = context.change();
        var component = TemplateRenderer.render(template, TemplateRenderer.pairs(
            "player", change.after().name(),
            "uuid", change.after().uuid().toString(),
            "lives", change.after().lives(),
            "maximum", context.maximumLives()
        ));
        switch (target) {
            case VICTIM -> {
                var player = context.player().getPlayer();
                if (player != null) {
                    player.sendMessage(component);
                }
            }
            case BROADCAST -> {
                for (var online : org.bukkit.Bukkit.getOnlinePlayers()) {
                    online.sendMessage(component);
                }
                org.bukkit.Bukkit.getConsoleSender().sendMessage(component);
            }
        }
    }

    private enum Target {
        VICTIM,
        BROADCAST;

        static List<String> names() {
            return Arrays.stream(values()).map(Enum::name).toList();
        }
    }
}
