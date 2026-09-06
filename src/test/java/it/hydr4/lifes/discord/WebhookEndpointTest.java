package it.hydr4.lifes.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.ConfigException;
import org.junit.jupiter.api.Test;

class WebhookEndpointTest {
    private static final String URL =
        "https://discord.com/api/webhooks/1234567890123456789/secret-token-abc";

    @Test
    void parsesAWellFormedWebhook() {
        var endpoint = WebhookEndpoint.parse(URL, "death.actions[0].webhook");
        assertEquals(URL, endpoint.url());
    }

    @Test
    void redactedKeepsTheIdButNeverTheToken() {
        var endpoint = WebhookEndpoint.parse(URL, "death.actions[0].webhook");
        assertTrue(endpoint.redacted().contains("1234567890123456789"), endpoint.redacted());
        assertFalse(endpoint.redacted().contains("secret-token-abc"), endpoint.redacted());
    }

    @Test
    void plainHttpIsRejected() {
        var exception = assertThrows(ConfigException.class, () -> WebhookEndpoint.parse(
            "http://discord.com/api/webhooks/1234567890123456789/secret-token-abc",
            "death.actions[0].webhook"));
        assertEquals("death.actions[0].webhook: must start with https://", exception.getMessage());
    }

    @Test
    void foreignHostsAreRejected() {
        var exception = assertThrows(ConfigException.class, () -> WebhookEndpoint.parse(
            "https://example.com/api/webhooks/1234567890123456789/secret-token-abc",
            "death.actions[0].webhook"));
        assertTrue(exception.getMessage().startsWith("death.actions[0].webhook: host must be one of"),
            exception.getMessage());
    }

    @Test
    void nonNumericIdsAreRejected() {
        var exception = assertThrows(ConfigException.class, () -> WebhookEndpoint.parse(
            "https://discord.com/api/webhooks/not-an-id/secret-token-abc",
            "death.actions[0].webhook"));
        assertEquals("death.actions[0].webhook: expected the form /api/webhooks/<numeric id>/<token>",
            exception.getMessage());
    }

    @Test
    void missingTokensAreRejected() {
        var exception = assertThrows(ConfigException.class, () -> WebhookEndpoint.parse(
            "https://discord.com/api/webhooks/1234567890123456789",
            "death.actions[0].webhook"));
        assertEquals("death.actions[0].webhook: expected the form /api/webhooks/<numeric id>/<token>",
            exception.getMessage());
    }

    @Test
    void previewAndTestHostsAreAllowed() {
        for (var host : new String[] {"ptb.discord.com", "canary.discord.com", "discordapp.com"}) {
            var endpoint = WebhookEndpoint.parse(
                "https://" + host + "/api/webhooks/1234567890123456789/secret-token-abc",
                "death.actions[0].webhook");
            assertTrue(endpoint.redacted().startsWith("https://" + host + "/"), endpoint.redacted());
            assertFalse(endpoint.redacted().contains("secret-token-abc"), endpoint.redacted());
        }
    }

    @Test
    void discordFlagsInTheQueryStringSurvive() {
        // A components-only payload is rejected with 400/50006 unless the webhook opts into
        // components, so the query string must reach Discord untouched.
        var endpoint = WebhookEndpoint.parse(
            "https://discord.com/api/webhooks/1234567890123456789/secret-token-abc?with_components=true",
            "death.actions[0].webhook");
        assertEquals(
            "https://discord.com/api/webhooks/1234567890123456789/secret-token-abc?with_components=true",
            endpoint.url());
        assertEquals(
            "https://discord.com/api/webhooks/1234567890123456789/***token redacted***?with_components=true",
            endpoint.redacted());
        assertFalse(endpoint.redacted().contains("secret-token-abc"), endpoint.redacted());
    }
}
