package it.hydr4.lifes.discord;

import java.util.logging.Logger;

/**
 * Test support: a gateway wired to nothing, for tests that build a runtime without exercising
 * delivery. The worker thread is created on the first submission, so an unused gateway is free.
 */
public final class OfflineGateway {
    private OfflineGateway() {
    }

    public static DiscordGateway create() {
        return new DiscordGateway(new RecordingDiscordTransport(), Logger.getLogger("lifes-test"), 0);
    }
}
