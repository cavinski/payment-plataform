package com.caio.paymentplatform.charge.dto;

import com.caio.paymentplatform.charge.Charge;
import com.caio.paymentplatform.charge.ChargeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ChargeResponse(

    UUID id,
    BigDecimal amount,
    String currency,
    String description,
    ChargeStatus status,
    Instant createdAt,
    Instant updatedAt

) {

    public static ChargeResponse from(Charge charge) {
        return new ChargeResponse(
            charge.getId(),
            charge.getAmount(),
            charge.getCurrency(),
            charge.getDescription(),
            charge.getStatus(),
            charge.getCreatedAt(),
            charge.getUpdatedAt()
        );
    }
}