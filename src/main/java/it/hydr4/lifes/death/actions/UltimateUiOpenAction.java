package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import it.hydr4.lifes.hook.UltimateUiHook;

/** Opens an UltimateUI GUI for the victim (skipped silently when offline). */
public final class UltimateUiOpenAction implements LifesAction {
    private final String gui;
    private final boolean hud;
    private final boolean autoclose;
    private final UltimateUiHook hook;

    private UltimateUiOpenAction(String gui, boolean hud, boolean autoclose, UltimateUiHook hook) {
        this.gui = gui;
        this.hud = hud;
        this.autoclose = autoclose;
        this.hook = hook;
    }

    public static UltimateUiOpenAction from(DeathActionSpec spec, UltimateUiHook hook) {
        if (hook == null) {
            throw new ConfigException(spec.path(), "ULTIMATEUI_OPEN action needs UltimateUI, but it is not attached");
        }
        spec.requireKeys("gui", "hud", "autoclose");
        return new UltimateUiOpenAction(spec.requiredString("gui"),
            spec.optionalBoolean("hud", false), spec.optionalBoolean("autoclose", false), hook);
    }

    @Override
    public void execute(ActionContext context) {
        var player = context.player().getPlayer();
        if (player != null) {
            hook.openGui(player, gui, hud, autoclose);
        }
    }
}
