package it.hydr4.lifes.discord;

import java.util.Objects;

/** One prepared webhook delivery: where it goes, what it says, and a label for the log. */
public record DiscordMessage(WebhookEndpoint endpoint, String body, String label) {
    public DiscordMessage {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(label, "label");
    }
}
