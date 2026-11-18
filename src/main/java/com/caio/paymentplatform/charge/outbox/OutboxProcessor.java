
package com.caio.paymentplatform.charge.outbox;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.caio.paymentplatform.charge.events.ChargePaidEvent;
import com.caio.paymentplatform.notification.PaymentNotificationHandler;

@Service
public class OutboxProcessor {

    private static final String CHARGE_PAID_EVENT = "ChargePaidEvent";

    private final OutboxRepository outboxRepository;
    private final PaymentNotificationHandler notificationHandler;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(
        OutboxRepository outboxRepository,
        PaymentNotificationHandler notificationHandler,
        ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.notificationHandler = notificationHandler;
        this.objectMapper = objectMapper;
    }

    
    @Transactional
    public void process(UUID eventId) {

        OutboxEvent event = outboxRepository.findByIdForUpdateSkipLocked(eventId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Outbox event not found or currently locked: " + eventId
            ));

        if (event.isProcessed()) {
            return;
        }

        processEvent(event);
    }

    @Transactional
    public boolean processNext() {

        Optional<OutboxEvent> optionalEvent = outboxRepository.findNextPendingForUpdate();

        if (optionalEvent.isEmpty()) {
            return false;
        }

        OutboxEvent event = optionalEvent.get();

        try {
            processEvent(event);
            return true;

        } catch (RuntimeException exception) {
            throw new OutboxProcessingException(event.getId(), exception);
        }
    }

    private void processEvent(OutboxEvent event) {

        switch (event.getEventType()) {

            case CHARGE_PAID_EVENT -> processChargePaidEvent(event);

            default -> throw new IllegalArgumentException(
                "Unsupported outbox event type: " + event.getEventType()
            );
        }

        event.markAsProcessed();
    }

    private void processChargePaidEvent(OutboxEvent outboxEvent) {

        try {

            ChargePaidEvent event = objectMapper.readValue(
                outboxEvent.getPayload(),
                ChargePaidEvent.class
            );

            notificationHandler.handle(event);

        } catch (JacksonException exception) {

            throw new IllegalStateException(
                "Could not deserialize charge paid event", exception
            );
        }
    }
}
