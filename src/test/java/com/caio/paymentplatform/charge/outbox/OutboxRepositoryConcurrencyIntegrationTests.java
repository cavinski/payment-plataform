
package com.caio.paymentplatform.charge.outbox;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class OutboxRepositoryConcurrencyIntegrationTests {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void shouldSkipEventAlreadyLockedByAnotherWorker() throws Exception {

        OutboxEvent event = OutboxEvent.create(
            UUID.randomUUID(),
            "ChargePaidEvent",
            "{}"
        );

        outboxRepository.saveAndFlush(event);

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {

            Future<Optional<OutboxEvent>> firstWorker = executor.submit(
                () -> transactionTemplate.execute(status -> {
                    Optional<OutboxEvent> selected = outboxRepository.findByIdForUpdateSkipLocked(event.getId());

                    lockAcquired.countDown();

                    try {

                        if (!releaseLock.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting to release database lock");
                        }
                        
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }

                    return selected;
                })
            );

            assertTrue(
                lockAcquired.await(5, TimeUnit.SECONDS),
                "First worker did not acquire the lock"
            );

            Optional<OutboxEvent> secondWorker = transactionTemplate.execute(
                status -> outboxRepository.findByIdForUpdateSkipLocked(event.getId())
            );

            assertTrue(
                secondWorker.isEmpty(),
                "Second worker should skip the locked event"
            );

            releaseLock.countDown();

            Optional<OutboxEvent> firstResult = firstWorker.get(5, TimeUnit.SECONDS);

            assertTrue(firstResult.isPresent());
            assertEquals(event.getId(), firstResult.get().getId());

        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
        }
    }
}
