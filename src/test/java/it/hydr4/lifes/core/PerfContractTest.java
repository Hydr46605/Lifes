package it.hydr4.lifes.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.api.LifeChangeReason;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PerfContractTest {
    @Test
    void applyDeathSustainsAtLeast100kOpsPerSecond() {
        var service = new DefaultLivesService(new AccountDirectory(), PerfContractTest::settings);
        var deaths = new AtomicLong();
        var id = UUID.randomUUID();
        service.create(id, "Perf");
        // Keep the balance high so the counter keeps moving for the whole run.
        service.adjust(id, LifeChangeReason.ADMIN_SET, 100_000);
        service.adjust(id, LifeChangeReason.ADMIN_ADD, 500_000);

        var deadline = System.nanoTime() + 500_000_000L;
        while (System.nanoTime() < deadline) {
            service.adjust(id, LifeChangeReason.ADMIN_REMOVE, 1);
            deaths.incrementAndGet();
        }
        var opsPerSecond = deaths.get() * 2; // wall clock is exactly 0.5s
        assertTrue(opsPerSecond >= 100_000, "only " + opsPerSecond + " ops/s");
    }

    @Test
    void concurrentAdjustmentsStayConsistent() throws InterruptedException {
        var service = new DefaultLivesService(new AccountDirectory(), PerfContractTest::settings);
        var id = UUID.randomUUID();
        service.create(id, "Concurrent");
        service.adjust(id, LifeChangeReason.ADMIN_SET, 100_000);

        var threads = 8;
        var perThread = 2_000;
        var latch = new CountDownLatch(threads);
        for (var index = 0; index < threads; index++) {
            Thread.ofPlatform().start(() -> {
                for (var round = 0; round < perThread; round++) {
                    service.adjust(id, LifeChangeReason.ADMIN_REMOVE, 1);
                }
                latch.countDown();
            });
        }
        latch.await();
        assertEquals(100_000 - threads * perThread, service.find(id).orElseThrow().lives());
    }

    private static it.hydr4.lifes.config.LivesSettings settings() {
        return new it.hydr4.lifes.config.LivesSettings(
            3, 100_000, 1, java.util.Set.of(), java.util.List.of(), java.util.List.of(), 0, true,
            it.hydr4.lifes.text.MessageTemplates.withOverrides(Map.of()));
    }
}
