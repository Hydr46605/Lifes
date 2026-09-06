package it.hydr4.lifes.death;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.actions.DiscordAction;
import it.hydr4.lifes.discord.DiscordGateway;
import it.hydr4.lifes.discord.RecordingDiscordTransport;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

class DiscordActionTest {
    private static final String WEBHOOK =
        "https://discord.com/api/webhooks/1234567890123456789/secret-token-abc";

    @Test
    void rendersPlaceholdersAndSubmitsOneSelfContainedMessage() throws InterruptedException {
        var transport = new RecordingDiscordTransport().respond(204, "");
        var gateway = new DiscordGateway(transport, Logger.getLogger("lifes-test"), 0);
        try {
            var action = DiscordAction.from(spec(
                "{\"content\": \"{player} died ({reason}): {lives}/{maximum}, deaths {deaths}, delta {delta}, {uuid}\"}"),
                gateway);
            action.execute(context());
            awaitCalls(transport, 1);
            var sent = transport.sent().get(0);
            assertTrue(sent.body().contains("Steve died (DEATH): 2/10, deaths 5, delta -1, "),
                sent.body());
            assertTrue(sent.body().contains("123e4567-e89b-12d3-a456-426614174000"), sent.body());
            assertEquals(WEBHOOK, sent.endpoint().url());
            assertFalse(sent.endpoint().redacted().contains("secret-token-abc"), sent.endpoint().redacted());
        } finally {
            gateway.close();
        }
    }

    @Test
    void missingGatewayFailsWithTheActionPath() {
        var exception = org.junit.jupiter.api.Assertions.assertThrows(ConfigException.class,
            () -> DiscordAction.from(spec("{\"content\": \"hi\"}"), null));
        assertEquals("death.actions[0]: DISCORD action needs a Discord gateway, but none was wired",
            exception.getMessage());
    }

    private static DeathActionSpec spec(String payload) {
        return new DeathActionSpec("DISCORD",
            Map.of("webhook", WEBHOOK, "payload", payload),
            "death.actions[0]");
    }

    private static ActionContext context() {
        var id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        var before = new LivesAccount(id, "Steve", 3, 4, Instant.parse("2026-09-05T10:00:00Z"), false);
        var after = new LivesAccount(id, "Steve", 2, 5, Instant.parse("2026-09-06T10:00:00Z"), false);
        // DiscordAction never touches the Bukkit player: everything it sends is rendered here, on the
        // calling thread, so a hollow proxy is enough and no server is needed.
        var player = (OfflinePlayer) Proxy.newProxyInstance(
            DiscordActionTest.class.getClassLoader(),
            new Class<?>[] {OfflinePlayer.class},
            (proxy, method, args) -> switch (method.getReturnType().getName()) {
                case "boolean" -> false;
                case "int", "long", "float", "double", "byte", "short" -> 0;
                default -> null;
            });
        return new ActionContext(new LifeChange(LifeChangeReason.DEATH, before, after, -1), player, 10);
    }

    private static void awaitCalls(RecordingDiscordTransport transport, int expected)
        throws InterruptedException {
        var deadline = System.currentTimeMillis() + 5_000;
        while (transport.calls() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, transport.calls());
    }
}
