package com.caio.paymentplatform.charge.exceptions;

import java.util.UUID;

public class ChargeNotFoundException extends RuntimeException{
    
    public ChargeNotFoundException(UUID id) {
        super("Charge not found: " + id);
    }
}