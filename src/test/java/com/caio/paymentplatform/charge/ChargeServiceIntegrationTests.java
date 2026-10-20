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

        ChargeResponse paid = chargeService.markAsPaid(created.id());
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

        chargeService.markAsPaid(created.id());

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
            () -> chargeService.markAsPaid(unknownId)
        );
    }
}