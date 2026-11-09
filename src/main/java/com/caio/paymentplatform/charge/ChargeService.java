package com.caio.paymentplatform.charge;

import com.caio.paymentplatform.charge.dto.ChargeResponse;
import com.caio.paymentplatform.charge.dto.CreateChargeRequest;
import com.caio.paymentplatform.charge.exceptions.ChargeNotFoundException;
import com.caio.paymentplatform.charge.idempotency.IdempotencyRecord;
import com.caio.paymentplatform.charge.idempotency.IdempotencyService;
import com.caio.paymentplatform.charge.outbox.OutboxService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.caio.paymentplatform.charge.events.ChargePaidEvent;
import java.util.UUID;

@Service 
public class ChargeService {
    
    private final ChargeRepository chargeRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    public ChargeService(
        ChargeRepository chargeRepository, 
        IdempotencyService idempotencyService, 
        ObjectMapper objectMapper,
        OutboxService outboxService
    ) {

        this.chargeRepository = chargeRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }

    @Transactional 
    public ChargeResponse create(CreateChargeRequest request) {

        Charge charge = Charge.create(request.amount(), request.description());

        Charge savedCharge = chargeRepository.save(charge);

        return ChargeResponse.from(savedCharge);
    }

    @Transactional(readOnly = true)
    public ChargeResponse findById(UUID id) {

        return ChargeResponse.from(findCharge(id));
    }

    @Transactional public ChargeResponse markAsPaid(UUID id, String idempotencyKey) {

        String requestHash = idempotencyService.generateRequestHash(
            "POST",
            "/api/v1/charges/" + id + "/pay"
        );

        IdempotencyRecord record = idempotencyService.registerOrRetrieve(idempotencyKey, requestHash);

        if (record.isCompleted()) {
            try {
                return objectMapper.readValue(record.getResponseBody(), ChargeResponse.class);
            } catch (JacksonException execption) {
                throw new IllegalStateException("Could not deserialize stored idempotency response", execption);
            }
        }

        Charge charge = findCharge(id);
        charge.markAsPaid();

        ChargeResponse response = ChargeResponse.from(chargeRepository.save(charge));

        ChargePaidEvent event = ChargePaidEvent.from(charge);
        outboxService.save(event);

        try {
            String responseBody = objectMapper.writeValueAsString(response);

            record.complete(200, responseBody);

        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize idempotency response", exception);
        }

        return response;
    }

    @Transactional public ChargeResponse cancel(UUID id) {

        Charge charge = findCharge(id);
        charge.cancel();

        return ChargeResponse.from(chargeRepository.save(charge));
    }

    @Transactional public ChargeResponse expire(UUID id) {

        Charge charge = findCharge(id);
        charge.expire();

        return ChargeResponse.from(chargeRepository.save(charge));
    }

    private Charge findCharge(UUID id) {
        return chargeRepository.findById(id)
            .orElseThrow(() -> new ChargeNotFoundException(id));
    }
}