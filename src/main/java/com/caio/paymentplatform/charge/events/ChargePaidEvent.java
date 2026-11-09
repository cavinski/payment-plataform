package com.caio.paymentplatform.charge.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.caio.paymentplatform.charge.Charge;

public record ChargePaidEvent(

    UUID eventId,
    UUID chargeId,
    BigDecimal amount,
    String currency,
    Instant occurredAt

) {

    public static ChargePaidEvent from(Charge charge) {
        return new ChargePaidEvent(
            UUID.randomUUID(),
            charge.getId(),
            charge.getAmount(),
            charge.getCurrency(),
            Instant.now()
        );
    }
}