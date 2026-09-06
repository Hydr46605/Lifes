package it.hydr4.lifes.discord;

import it.hydr4.lifes.ConfigException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A Discord webhook address, validated once and never printed with its token.
 *
 * <p>The token in the path is the entire credential, so the redacted form is the only one this
 * class hands out for logging. Restricting the host is not ceremony: a mistyped address would
 * otherwise deliver player names to somebody else's endpoint in silence.
 */
public record WebhookEndpoint(String url, String redacted) {
    private static final Set<String> ALLOWED_HOSTS = Set.of(
        "discord.com",
        "ptb.discord.com",
        "canary.discord.com",
        "discordapp.com"
    );
    private static final Pattern WEBHOOK_PATH = Pattern.compile("^/api/webhooks/(\\d{1,32})/([^/]{1,256})$");

    public WebhookEndpoint {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(redacted, "redacted");
    }

    /** Parses and validates the value, failing with the configuration path that holds it. */
    public static WebhookEndpoint parse(String raw, String path) {
        URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (URISyntaxException exception) {
            throw new ConfigException(path, "is not a valid URL: " + exception.getReason());
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new ConfigException(path, "must start with https://");
        }
        var host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!ALLOWED_HOSTS.contains(host)) {
            throw new ConfigException(path, "host must be one of " + ALLOWED_HOSTS + ", got '" + host + "'");
        }
        var webhookPath = uri.getRawPath() == null ? "" : uri.getRawPath();
        var matcher = WEBHOOK_PATH.matcher(webhookPath);
        if (!matcher.matches()) {
            throw new ConfigException(path, "expected the form /api/webhooks/<numeric id>/<token>");
        }
        // The query string carries no secret (the token lives in the path) but Discord reads
        // flags from it: a components-only payload needs ?with_components=true, and forum
        // channels need ?thread_id=..., so dropping it would silently misdeliver.
        var rawQuery = uri.getRawQuery();
        var suffix = rawQuery == null || rawQuery.isBlank() ? "" : "?" + rawQuery;
        return new WebhookEndpoint(
            "https://" + host + webhookPath + suffix,
            "https://" + host + "/api/webhooks/" + matcher.group(1) + "/***token redacted***" + suffix
        );
    }
}
