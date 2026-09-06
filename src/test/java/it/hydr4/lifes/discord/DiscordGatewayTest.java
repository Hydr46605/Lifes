package it.hydr4.lifes.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class DiscordGatewayTest {
    private static final WebhookEndpoint ENDPOINT = WebhookEndpoint.parse(
        "https://discord.com/api/webhooks/1234567890123456789/secret-token-abc",
        "death.actions[0].webhook");

    @Test
    void successPostsOnceAndReturns() throws InterruptedException {
        var transport = new RecordingDiscordTransport().respond(204, "");
        var gateway = gateway(transport, 2);
        try {
            gateway.submit(message("hello"));
            awaitCalls(transport, 1);
            assertEquals("hello", transport.sent().get(0).body());
        } finally {
            gateway.close();
        }
    }

    @Test
    void rateLimitIsRetriedThenDelivered() throws InterruptedException {
        var transport = new RecordingDiscordTransport()
            .respond(429, "{\"retry_after\": 0.01}")
            .respond(204, "");
        var gateway = gateway(transport, 2);
        try {
            gateway.submit(message("retry me"));
            awaitCalls(transport, 2);
        } finally {
            gateway.close();
        }
    }

    @Test
    void exhaustedRetriesStopAtOneAttempt() throws InterruptedException {
        var transport = new RecordingDiscordTransport().respond(500, "discord is down");
        var gateway = gateway(transport, 0);
        try {
            gateway.submit(message("doomed"));
            awaitCalls(transport, 1);
            Thread.sleep(100);
            assertEquals(1, transport.calls());
        } finally {
            gateway.close();
        }
    }

    @Test
    void unreachableHostsAreDroppedWithoutARetryStorm() throws InterruptedException {
        var transport = new RecordingDiscordTransport().unreachable("no route to host");
        var gateway = gateway(transport, 0);
        try {
            gateway.submit(message("lost"));
            awaitCalls(transport, 1);
            Thread.sleep(100);
            assertEquals(1, transport.calls());
        } finally {
            gateway.close();
        }
    }

    @Test
    void overflowBeyondThirtyTwoInFlightIsDropped() {
        var transport = new BlockingTransport();
        var gateway = gateway(transport, 0);
        try {
            for (var index = 0; index < 32; index++) {
                gateway.submit(message("bulk-" + index));
            }
            gateway.submit(message("one too many"));
            transport.release();
        } finally {
            gateway.close();
        }
        assertEquals(32, transport.calls.get());
    }

    @Test
    void closeDrainsWhatWasQueued() {
        var transport = new RecordingDiscordTransport().respond(204, "");
        var gateway = gateway(transport, 0);
        gateway.submit(message("last words"));
        gateway.close();
        assertEquals(1, transport.calls());
    }

    @Test
    void outOfRangeRetriesAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> gateway(new RecordingDiscordTransport(), 6));
    }

    private static DiscordGateway gateway(DiscordTransport transport, int retries) {
        return new DiscordGateway(transport, Logger.getLogger("lifes-test"), retries);
    }

    private static DiscordMessage message(String body) {
        return new DiscordMessage(ENDPOINT, body, "test");
    }

    private static void awaitCalls(RecordingDiscordTransport transport, int expected)
        throws InterruptedException {
        var deadline = System.currentTimeMillis() + 5_000;
        while (transport.calls() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, transport.calls());
        assertTrue(transport.sent().size() >= expected);
    }

    /** Blocks every delivery until the test releases it, so the queue can be filled on purpose. */
    private static final class BlockingTransport implements DiscordTransport {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch gate = new CountDownLatch(1);

        void release() {
            gate.countDown();
        }

        @Override
        public Response post(WebhookEndpoint endpoint, String body) throws Failure {
            calls.incrementAndGet();
            try {
                gate.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new Failure("interrupted while blocked in test", exception);
            }
            return new Response(204, "");
        }
    }
}
