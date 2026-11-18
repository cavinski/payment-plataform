package com.caio.paymentplatform.notification;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, UUID> {

    Optional<PaymentNotification> findByEventId(UUID eventId);
}