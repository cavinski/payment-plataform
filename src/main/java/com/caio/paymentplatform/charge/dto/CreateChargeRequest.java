package com.caio.paymentplatform.charge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateChargeRequest (
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be att least 0.01")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer digits and 2 decimal places")
    BigDecimal amount,

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description

) {}
