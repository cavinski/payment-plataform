
package com.caio.paymentplatform.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
class PaymentNotificationRepositoryIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private PaymentNotificationRepository notificationRepository;

    @Test
    void shouldPersistAndRetrieveNotificationByEventId() {

        UUID eventId = UUID.randomUUID();
        UUID chargeId = UUID.randomUUID();

        PaymentNotification notification = PaymentNotification.create(
            eventId,
            chargeId,
            "CHARGE_PAID",
            "Your payment was successfully confirmed."
        );

        PaymentNotification saved = notificationRepository.saveAndFlush(notification);

        var found = notificationRepository.findByEventId(eventId);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(eventId, found.get().getEventId());
        assertEquals(chargeId, found.get().getChargeId());
        assertEquals("CHARGE_PAID", found.get().getNotificationType());
    }

    @Test
    void shouldReturnEmptyWhenEventIdDoesNotExist() {
        
        var found = notificationRepository.findByEventId(
            UUID.randomUUID()
        );

        assertTrue(found.isEmpty());
    }
}
