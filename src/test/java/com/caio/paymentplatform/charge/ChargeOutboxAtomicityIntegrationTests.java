package com.caio.paymentplatform.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import com.caio.paymentplatform.charge.dto.ChargeResponse;
import com.caio.paymentplatform.charge.dto.CreateChargeRequest;
import com.caio.paymentplatform.charge.outbox.OutboxEvent;
import com.caio.paymentplatform.charge.outbox.OutboxRepository;

@SpringBootTest
@ActiveProfiles("test")
class ChargeOutboxAtomicityIntegrationTests {

    @Autowired
    private ChargeService chargeService;

    @Autowired
    private ChargeRepository chargeRepository;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @Test
    void shouldRollbackChargePaymentWhenOutboxPersistenceFails() {

        ChargeResponse created = chargeService.create(
            new CreateChargeRequest(
                new BigDecimal("149.90"),
                "Outbox atomicity test"
            )
        );

        doThrow(new DataIntegrityViolationException(
            "Simulated outbox persistence failure"
        )).when(outboxRepository).save(any(OutboxEvent.class));

        assertThrows(
            DataIntegrityViolationException.class,
            () -> chargeService.markAsPaid(
                created.id(),
                "atomicity-" + UUID.randomUUID()
            )
        );

        Charge charge = chargeRepository.findById(created.id()).orElseThrow();

        assertEquals(ChargeStatus.PENDING, charge.getStatus());
    }
}