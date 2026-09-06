package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import it.hydr4.lifes.discord.DiscordGateway;
import it.hydr4.lifes.discord.DiscordMessage;
import it.hydr4.lifes.discord.DiscordPayload;
import it.hydr4.lifes.discord.WebhookEndpoint;
import it.hydr4.lifes.text.TemplateRenderer;

/**
 * Announces a life change to a Discord webhook.
 *
 * <p>Everything read from Bukkit is read here, on the calling thread, and turned into an immutable
 * message before the gateway sees it. The worker thread never touches a player object.
 */
public final class DiscordAction implements LifesAction {
    private final WebhookEndpoint endpoint;
    private final DiscordPayload payload;
    private final DiscordGateway gateway;

    private DiscordAction(WebhookEndpoint endpoint, DiscordPayload payload, DiscordGateway gateway) {
        this.endpoint = endpoint;
        this.payload = payload;
        this.gateway = gateway;
    }

    public static DiscordAction from(DeathActionSpec spec, DiscordGateway gateway) {
        if (gateway == null) {
            throw new ConfigException(spec.path(), "DISCORD action needs a Discord gateway, but none was wired");
        }
        spec.requireKeys("webhook", "payload");
        var endpoint = WebhookEndpoint.parse(spec.requiredString("webhook"), spec.path("webhook"));
        var payload = DiscordPayload.compile(spec.requiredString("payload"), spec.path("payload"));
        return new DiscordAction(endpoint, payload, gateway);
    }

    @Override
    public void execute(ActionContext context) {
        var change = context.change();
        var account = change.after();
        var body = payload.render(TemplateRenderer.pairs(
            "player", account.name(),
            "uuid", account.uuid().toString(),
            "lives", account.lives(),
            "maximum", context.maximumLives(),
            "deaths", account.totalDeaths(),
            "reason", change.reason().name(),
            "delta", change.delta()
        ));
        gateway.submit(new DiscordMessage(endpoint, body, account.name()));
    }
}
