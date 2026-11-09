package com.caio.paymentplatform.charge.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.caio.paymentplatform.charge.events.ChargePaidEvent;

@Service 
public class OutboxService {
    
    private final OutboxRepository outboxRepository;
    
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(ChargePaidEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.create(
                event.chargeId(),
                event.getClass().getSimpleName(),
                payload
            );

            outboxRepository.save(outboxEvent);

        } catch (JacksonException exception) {

            throw new IllegalStateException("Could not serialize charge paid event", exception);        
        }
    }
}