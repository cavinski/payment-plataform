package com.caio.paymentplatform.charge.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;



@SpringBootTest 
@Testcontainers
public class IdempotencyServiceIntegrationTests {
    
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldRegisterNewIdempotencyKey() {

        String key = "payment-request-001";
        String hash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/" + UUID.randomUUID() + "/pay"
        );

        IdempotencyRecord record = transactionTemplate().execute(status ->
            idempotencyService.registerOrRetrieve(key, hash));

        assertThat(record).isNotNull();
        assertThat(record.isCompleted()).isFalse();
        assertThat(record.getRequestHash()).isEqualTo(hash);
    }

    @Test
    void shouldRetrieveExistingCompletedIdempotencyKey() {

        String key = "payment-request-002";
        String hash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/123/pay"
        );

        transactionTemplate().executeWithoutResult(status -> {
            IdempotencyRecord record = idempotencyService.registerOrRetrieve(key, hash);

            record.complete(200, "{\"status\":\"PAID\"}");
        });

        IdempotencyRecord record = transactionTemplate().execute(status ->
            idempotencyService.registerOrRetrieve(key, hash));

        assertThat(record).isNotNull();
        assertThat(record.isCompleted()).isTrue();
        assertThat(record.getResponseStatus()).isEqualTo(200);
        assertThat(record.getResponseBody()).isEqualTo("{\"status\":\"PAID\"}");
    }

    @Test
    void shouldRejectSameKeyWithDifferentRequestHash() {

        String key = "payment-request-003";

        String originalHash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/123/pay"
        );

        String differentHash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/456/pay"
        );

        transactionTemplate().execute(status ->
            idempotencyService.registerOrRetrieve(key, originalHash)
        );

        assertThatThrownBy(() -> transactionTemplate().execute(status -> 
            idempotencyService.registerOrRetrieve(key, differentHash)))
            .isInstanceOf(IdempotencyKeyConflictException.class);
    }

    @Test
    void shouldGenerateSameHashForSameRequest() {

        String firstHash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/123/pay"
        );

        String secondHash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/123/pay"
        );

        assertThat(firstHash).isEqualTo(secondHash);
        assertThat(firstHash).hasSize(64);
    }

    @Test
    void shouldGenerateDifferentHashesForDifferentRequests() {

        String firstHash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/123/pay"
        );

        String secondHash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/456/pay"
        );

        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @Test
    void shouldHandleConcurrentRequestsWithSameIdempotencyKey() throws Exception {
        
        String key = "payment-request-" + UUID.randomUUID();

        String hash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/123/pay"
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger executionCount = new AtomicInteger();

        Callable<IdempotencyResult> operation = () -> {

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not start");
            }

            return transactionTemplate().execute(status -> {

                IdempotencyRecord record = idempotencyService.registerOrRetrieve(key, hash);

                if (!record.isCompleted()) {
                    executionCount.incrementAndGet();

                    record.complete(200, "{\"status\":\"PAID\"}");
                }

                return new IdempotencyResult(
                    record.getResponseStatus(),
                    record.getResponseBody()
                );
            });
        };

        try {

            Future<IdempotencyResult> firstRequest = executor.submit(operation);

            Future<IdempotencyResult> secondRequest = executor.submit(operation);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            IdempotencyResult firstResult = firstRequest.get(30, TimeUnit.SECONDS);

            IdempotencyResult secondResult = secondRequest.get(30, TimeUnit.SECONDS);

            assertThat(executionCount.get()).isEqualTo(1);

            assertThat(firstResult.responseStatus()).isEqualTo(200);
            assertThat(secondResult.responseStatus()).isEqualTo(200);

            assertThat(firstResult.responseBody())
                .isEqualTo(secondResult.responseBody())
                .isEqualTo("{\"status\":\"PAID\"}");

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private record IdempotencyResult(
        Integer responseStatus,
        String responseBody
    ) {
    }
}