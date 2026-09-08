package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import it.hydr4.lifes.hook.UltimateUiHook;
import it.hydr4.lifes.text.TemplateRenderer;

/** Sets a text element of the victim's open UltimateUI GUI with substituted placeholders. */
public final class UltimateUiSetAction implements LifesAction {
    private final String element;
    private final String template;
    private final UltimateUiHook hook;

    private UltimateUiSetAction(String element, String template, UltimateUiHook hook) {
        this.element = element;
        this.template = template;
        this.hook = hook;
    }

    public static UltimateUiSetAction from(DeathActionSpec spec, UltimateUiHook hook) {
        if (hook == null) {
            throw new ConfigException(spec.path(), "ULTIMATEUI_SET action needs UltimateUI, but it is not attached");
        }
        spec.requireKeys("element", "text");
        return new UltimateUiSetAction(spec.requiredString("element"), spec.requiredString("text"), hook);
    }

    @Override
    public void execute(ActionContext context) {
        var player = context.player().getPlayer();
        if (player == null) {
            return;
        }
        var change = context.change();
        var text = TemplateRenderer.renderPlain(template, TemplateRenderer.pairs(
            "player", change.after().name(),
            "uuid", change.after().uuid().toString(),
            "lives", change.after().lives(),
            "maximum", context.maximumLives(),
            "deaths", change.after().totalDeaths(),
            "delta", change.delta(),
            "before", change.before().lives(),
            "reason", change.reason().name()
        ));
        hook.setElementText(player, element, text);
    }
}
