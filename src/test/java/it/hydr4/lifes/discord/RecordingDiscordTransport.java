package it.hydr4.lifes.discord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test support: answers with queued responses and records what it was handed.
 *
 * <p>The last queued response repeats, so a test can queue one 429 and one 204 and watch the retry
 * consume them in order.
 */
public final class RecordingDiscordTransport implements DiscordTransport {
    private final List<DiscordMessage> sent = Collections.synchronizedList(new ArrayList<>());
    private final List<Response> replies = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger calls = new AtomicInteger();
    private volatile String failure;

    public RecordingDiscordTransport respond(int status, String body) {
        replies.add(new Response(status, body));
        return this;
    }

    /** Makes every call fail the way an unreachable host would. */
    public RecordingDiscordTransport unreachable(String reason) {
        failure = reason;
        return this;
    }

    public List<DiscordMessage> sent() {
        return List.copyOf(sent);
    }

    public int calls() {
        return calls.get();
    }

    @Override
    public Response post(WebhookEndpoint endpoint, String body) throws Failure {
        var call = calls.incrementAndGet();
        var message = new DiscordMessage(endpoint, body, "call-" + call);
        sent.add(message);
        var alwaysFails = failure;
        if (alwaysFails != null) {
            throw new Failure(alwaysFails, new java.net.ConnectException(alwaysFails));
        }
        var index = Math.min(call - 1, Math.max(replies.size() - 1, 0));
        return replies.isEmpty() ? new Response(204, "") : replies.get(index);
    }
}
