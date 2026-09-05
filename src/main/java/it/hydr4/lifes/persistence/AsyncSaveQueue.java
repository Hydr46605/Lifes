package it.hydr4.lifes.persistence;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coalescing save queue; periodic timer covers quiet servers. */
public final class AsyncSaveQueue implements LivesListener, AutoCloseable {
    private final LivesRepository repository;
    private final Supplier<Map<UUID, LivesAccount>> snapshot;
    private final boolean offThread;
    private final Logger logger;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean queued = new AtomicBoolean();
    private ScheduledFuture<?> periodicTask;

    public AsyncSaveQueue(
        LivesRepository repository,
        Supplier<Map<UUID, LivesAccount>> snapshot,
        boolean offThread,
        Logger logger
    ) {
        this.repository = repository;
        this.snapshot = snapshot;
        this.offThread = offThread;
        this.logger = logger;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "Lifes-Save");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Starts the periodic dirty flush; zero or negative disables it. */
    public void startPeriodic(int intervalSeconds) {
        if (intervalSeconds > 0) {
            periodicTask = executor.scheduleWithFixedDelay(this::flush, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onLifeChange(LifeChange change) {
        if (change.changed()) {
            enqueue();
        }
    }

    /** Writes the current snapshot once; safe to call from any thread. */
    public void flush() {
        try {
            repository.saveAll(snapshot.get());
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Saving lives accounts failed", exception);
        }
    }

    private void enqueue() {
        if (!offThread) {
            flush();
            return;
        }
        if (queued.compareAndSet(false, true)) {
            executor.execute(() -> {
                queued.set(false);
                flush();
            });
        }
    }

    @Override
    public void close() {
        if (periodicTask != null) {
            periodicTask.cancel(false);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        flush();
    }
}
