package it.hydr4.lifes.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.Gson;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns Discord delivery for the whole plugin: one transport, one worker, one budget.
 *
 * <p>Announcements are cosmetic, so this class is allowed to lose them and never to stall the
 * server. {@link #submit(DiscordMessage)} returns without touching the network, and the queue is
 * bounded so a mass-death event cannot grow the heap. The gateway outlives configuration reloads;
 * only the plugin disables it.
 */
public final class DiscordGateway implements AutoCloseable {
    /** Attempts beyond the first are only made for rate limits and server faults. */
    public static final int DEFAULT_RETRIES = 2;
    /** Messages this many attempts apart are still retried; beyond that they are dropped. */
    public static final int MAX_RETRIES = 5;
    private static final int MAX_IN_FLIGHT = 32;
    private static final long MAX_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final int BODY_IN_LOG = 400;

    private static final Gson JSON = new Gson();

    private final DiscordTransport transport;
    private final Logger logger;
    private final int retries;
    private final Semaphore budget;
    private final AtomicLong dropped = new AtomicLong();
    private final ExecutorService worker;
    private volatile boolean closed;

    public DiscordGateway(DiscordTransport transport, Logger logger, int retries) {
        if (retries < 0 || retries > MAX_RETRIES) {
            throw new IllegalArgumentException("retries must be between 0 and " + MAX_RETRIES + ": " + retries);
        }
        this.transport = Objects.requireNonNull(transport, "transport");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.retries = retries;
        this.budget = new Semaphore(MAX_IN_FLIGHT);
        this.worker = Executors.newSingleThreadExecutor(runnable -> {
            var thread = new Thread(runnable, "Lifes-Discord");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Queues one announcement. Never blocks and never throws: the caller is the main thread, in the
     * middle of a death event, and a slow Discord must not be its problem.
     */
    public void submit(DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        if (closed) {
            logger.warning("Discord announcement for " + message.label() + " dropped, the gateway is closed");
            return;
        }
        if (!budget.tryAcquire()) {
            noteBacklog(message);
            return;
        }
        try {
            worker.execute(() -> {
                try {
                    deliver(message);
                } finally {
                    budget.release();
                }
            });
        } catch (RejectedExecutionException exception) {
            budget.release();
            logger.warning("Discord announcement for " + message.label() + " dropped, the worker had stopped");
        }
    }

    /** Stops accepting work and gives what is already queued a short window to finish. */
    @Override
    public void close() {
        closed = true;
        worker.shutdown();
        try {
            if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
                worker.shutdownNow();
                logger.warning("Discord announcements were still in flight when the plugin disabled");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            worker.shutdownNow();
        }
    }

    private void deliver(DiscordMessage message) {
        for (var attempt = 0; attempt <= retries; attempt++) {
            var finalAttempt = attempt == retries;
            DiscordTransport.Response response = null;
            try {
                response = transport.post(message.endpoint(), message.body());
                if (response.successful()) {
                    return;
                }
                if (finalAttempt || !response.retryable()) {
                    reject(message, response);
                    return;
                }
            } catch (DiscordTransport.Failure exception) {
                if (finalAttempt) {
                    logger.log(Level.WARNING, "Discord announcement for " + message.label() + " failed: "
                        + exception.getMessage() + " (" + message.endpoint().redacted() + ")");
                    return;
                }
            }
            if (!sleepBefore(message, response, attempt)) {
                return;
            }
        }
    }

    private void reject(DiscordMessage message, DiscordTransport.Response response) {
        logger.warning("Discord rejected the announcement for " + message.label()
            + ": HTTP " + response.status() + " " + trim(response.body())
            + " (" + message.endpoint().redacted() + ")");
    }

    /** Waits out a rate limit; false means the thread was interrupted and the message is lost. */
    private boolean sleepBefore(DiscordMessage message, DiscordTransport.Response response, int attempt) {
        var millis = response != null && response.status() == 429
            ? retryAfterMillis(response.body())
            : TimeUnit.SECONDS.toMillis(attempt + 1L);
        try {
            Thread.sleep(Math.min(millis, MAX_WAIT_MILLIS));
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warning("Discord announcement for " + message.label() + " abandoned during its retry wait");
            return false;
        }
    }

    /** Discord says how long to wait; a malformed hint falls back to one second. */
    private long retryAfterMillis(String body) {
        try {
            var parsed = JSON.fromJson(body, JsonElement.class);
            if (parsed != null && parsed.isJsonObject() && parsed.getAsJsonObject().has("retry_after")) {
                var seconds = parsed.getAsJsonObject().get("retry_after").getAsDouble();
                return (long) Math.ceil(seconds * 1000.0);
            }
        } catch (JsonParseException | IllegalStateException exception) {
            logger.log(Level.FINE, "Discord sent a rate limit hint this parser could not read", exception);
        }
        return TimeUnit.SECONDS.toMillis(1);
    }

    private void noteBacklog(DiscordMessage message) {
        var lost = dropped.incrementAndGet();
        if (lost == 1 || lost % 20 == 0) {
            logger.warning("Discord announcements are backing up: " + lost + " dropped so far, most recently "
                + message.label() + ". Consider a dedicated channel or fewer death actions.");
        }
    }

    private static String trim(String body) {
        var flattened = body.replaceAll("\\s+", " ").trim();
        return flattened.length() <= BODY_IN_LOG ? flattened : flattened.substring(0, BODY_IN_LOG) + "...";
    }
}
