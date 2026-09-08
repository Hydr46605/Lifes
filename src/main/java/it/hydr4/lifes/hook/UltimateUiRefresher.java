package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LivesListener;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.text.TemplateRenderer;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Rewrites one UltimateUI text element after every life change while the configured GUI is
 * open. Reads settings live, so {@code /lives reload} applies without re-attaching.
 */
public final class UltimateUiRefresher implements LivesListener {
    private final UltimateUiHook hook;
    private final Supplier<LivesSettings> settings;

    public UltimateUiRefresher(UltimateUiHook hook, Supplier<LivesSettings> settings) {
        this.hook = Objects.requireNonNull(hook, "hook");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public void onLifeChange(LifeChange change) {
        var ui = settings.get().ultimateUi();
        if (!ui.enabled()) {
            return;
        }
        var player = Bukkit.getPlayer(change.after().uuid());
        if (player == null) {
            return;
        }
        var text = TemplateRenderer.renderPlain(ui.refreshText(), TemplateRenderer.pairs(
            "player", change.after().name(),
            "uuid", change.after().uuid().toString(),
            "lives", change.after().lives(),
            "maximum", settings.get().maximumLives(),
            "deaths", change.after().totalDeaths(),
            "delta", change.delta(),
            "before", change.before().lives(),
            "reason", change.reason().name()
        ));
        hook.setElementTextIfGui(player, ui.refreshGui(), ui.refreshElement(), text);
    }
}
