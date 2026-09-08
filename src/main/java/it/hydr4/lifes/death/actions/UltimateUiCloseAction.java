package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import it.hydr4.lifes.hook.UltimateUiHook;

/** Closes the victim's UltimateUI GUI, or only the named one when given. */
public final class UltimateUiCloseAction implements LifesAction {
    private final String guiOrNull;
    private final UltimateUiHook hook;

    private UltimateUiCloseAction(String guiOrNull, UltimateUiHook hook) {
        this.guiOrNull = guiOrNull;
        this.hook = hook;
    }

    public static UltimateUiCloseAction from(DeathActionSpec spec, UltimateUiHook hook) {
        if (hook == null) {
            throw new ConfigException(spec.path(), "ULTIMATEUI_CLOSE action needs UltimateUI, but it is not attached");
        }
        spec.requireKeys("gui");
        var gui = spec.options().get("gui");
        if (gui == null) {
            return new UltimateUiCloseAction(null, hook);
        }
        return new UltimateUiCloseAction(spec.requiredString("gui"), hook);
    }

    @Override
    public void execute(ActionContext context) {
        var player = context.player().getPlayer();
        if (player != null) {
            hook.closeGui(player, guiOrNull);
        }
    }
}
