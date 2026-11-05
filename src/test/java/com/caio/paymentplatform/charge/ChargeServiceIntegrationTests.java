package com.caio.paymentplatform.charge;

import com.caio.paymentplatform.charge.dto.ChargeResponse;
import com.caio.paymentplatform.charge.dto.CreateChargeRequest;
import com.caio.paymentplatform.charge.exceptions.ChargeNotFoundException;
import com.caio.paymentplatform.charge.exceptions.ChargeStateTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers 
@SpringBootTest 
class ChargeServiceIntegrationTests {
    
    @Container 
    @ServiceConnection 
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired 
    private ChargeService chargeService;

    @Autowired 
    private ChargeRepository chargeRepository;

    @Autowired 
    private PlatformTransactionManager transactionManager;

    @Autowired 
    private EntityManager entityManager;

    @Test
    void shouldCreateAndRetrieveCharge() {

        ChargeResponse created = chargeService.create(new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Portfolio integration test"
            )
        );

        assertNotNull(created.id());
        assertEquals(new BigDecimal("149.90"), created.amount());
        assertEquals("BRL", created.currency());
        assertEquals(ChargeStatus.PENDING, created.status());
        assertNotNull(created.createdAt());
        assertNotNull(created.updatedAt());

        ChargeResponse retrieved = chargeService.findById(created.id());

        assertEquals(created.id(), retrieved.id());
        assertEquals(created.amount(), retrieved.amount());
        assertEquals(created.status(), retrieved.status());
        assertEquals(created.description(), retrieved.description());

        assertTrue(chargeRepository.existsById(created.id()));
    }

    @Test
    void shouldRejectNonPositiveAmount() {

        assertThrows(IllegalArgumentException.class, () -> Charge.create(
                new BigDecimal("0.00"),
                "Invalid charge"
            )
        );

        assertThrows(IllegalArgumentException.class, () -> Charge.create(
                new BigDecimal("-10.00"),
                "Invalid charge"
            )
        );
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {

        assertThrows(IllegalArgumentException.class, () -> Charge.create(
                new BigDecimal("10.001"),
                "Invalid precision"
            )
        );
    }

    @Test
    void shouldThrowWhenChargeDoesNotExist() {

        UUID unknownId = UUID.randomUUID();

        assertThrows(ChargeNotFoundException.class, 
            () -> chargeService.findById(unknownId));
    }

    @Test
    void shouldPersistPaidStatus() {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Charge to pay"
            )
        );

        ChargeResponse paid = chargeService.markAsPaid(
            created.id(), "test-payment-" + UUID.randomUUID());

        ChargeResponse retrieved = chargeService.findById(created.id());

        assertEquals(ChargeStatus.PAID, paid.status());
        assertEquals(ChargeStatus.PAID, retrieved.status());
    }

    @Test
    void shouldPersistCancelledStatus() {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Charge to cancel"
            )
        );

        chargeService.cancel(created.id());

        ChargeResponse retrieved = chargeService.findById(created.id());

        assertEquals(ChargeStatus.CANCELLED, retrieved.status());
    }

    @Test
    void shouldPersistExpiredStatus() {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Charge to expire"
            )
        );

        chargeService.expire(created.id());

        ChargeResponse retrieved = chargeService.findById(created.id());

        assertEquals(ChargeStatus.EXPIRED, retrieved.status());
    }

    @Test
    void shouldNotChangePersistedStatusWhenTransitionIsInvalid() {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Charge with invalid transition"
            )
        );

        chargeService.markAsPaid(created.id(), "test-payment-" + UUID.randomUUID());

        assertThrows(
            ChargeStateTransitionException.class,
            () -> chargeService.cancel(created.id())
        );

        ChargeResponse retrieved = chargeService.findById(created.id());

        assertEquals(ChargeStatus.PAID, retrieved.status());
    }

    @Test
    void shouldThrowWhenTryingToTransitionNonexistentCharge() {
        
        UUID unknownId = UUID.randomUUID();

        assertThrows(
            ChargeNotFoundException.class,
            () -> chargeService.markAsPaid(unknownId, "test-payment-" + UUID.randomUUID())
        );
    }

    @Test
    void shouldAllowOnlyOneConcurrentChargeTransition() throws Exception {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Concurrent charge transition"
            )
        );

        CountDownLatch bothTransactionsLoaded = new CountDownLatch(2);
        CountDownLatch allowUpdates = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            Future<Throwable> payment = executor.submit(() ->
                attemptTransition(
                    created.id(),
                    true,
                    bothTransactionsLoaded,
                    allowUpdates
                )
            );

            Future<Throwable> cancellation = executor.submit(() ->
                attemptTransition(
                    created.id(),
                    false,
                    bothTransactionsLoaded,
                    allowUpdates
                )
            );

            assertTrue(
                bothTransactionsLoaded.await(10, TimeUnit.SECONDS),
                "Both transactions should load the charge"
            );

            allowUpdates.countDown();

            Throwable paymentFailure = payment.get(10, TimeUnit.SECONDS);
            Throwable cancellationFailure = cancellation.get(10, TimeUnit.SECONDS);

            boolean paymentSucceeded = paymentFailure == null;
            boolean cancellationSucceeded = cancellationFailure == null;

            assertTrue(
                paymentSucceeded ^ cancellationSucceeded,
                "Exactly one transition should succeed"
            );

            Throwable losingFailure = paymentSucceeded
                ? cancellationFailure
                : paymentFailure;

            assertTrue(isOptimisticLockFailure(losingFailure),
                () -> "Expected an optimistic locking failure, but got: " + losingFailure);

            ChargeResponse finalCharge = chargeService.findById(created.id());

            assertTrue(finalCharge.status() == ChargeStatus.PAID
                    || finalCharge.status() == ChargeStatus.CANCELLED);

        } finally {
            allowUpdates.countDown();
            executor.shutdownNow();
        }
    }

    private Throwable attemptTransition(
        
            UUID chargeId,
            boolean markAsPaid,
            CountDownLatch bothTransactionsLoaded,
            CountDownLatch allowUpdates) {

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try {

            transaction.executeWithoutResult(transactionStatus -> {

                Charge charge = chargeRepository.findById(chargeId).orElseThrow();

                bothTransactionsLoaded.countDown();
                await(allowUpdates);

                if (markAsPaid) {
                    charge.markAsPaid();
                } else {
                    charge.cancel();
                }

                entityManager.flush();
            });

            return null;

        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private void await(CountDownLatch latch) {

        try {

            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent transactions");
            }
            
        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for concurrent transactions",exception
            );
        }
    }

    private boolean isOptimisticLockFailure(Throwable exception) {

        Throwable current = exception;

        while (current != null) {
            
            if (current instanceof ObjectOptimisticLockingFailureException
                    || current instanceof OptimisticLockException
                    || current instanceof StaleStateException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}