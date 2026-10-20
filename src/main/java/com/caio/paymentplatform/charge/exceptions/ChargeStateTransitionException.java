package com.caio.paymentplatform.charge.exceptions;

import com.caio.paymentplatform.charge.ChargeStatus;

public class ChargeStateTransitionException extends RuntimeException{
 
    public ChargeStateTransitionException(ChargeStatus status) {
        super("Cannot change charge status from " + status);
    }
}