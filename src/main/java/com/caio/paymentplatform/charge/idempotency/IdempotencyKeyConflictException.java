package com.caio.paymentplatform.charge.idempotency;

public class IdempotencyKeyConflictException extends RuntimeException{
 
    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("Idempotency key was already used for a different request: " + idempotencyKey);
    }
}