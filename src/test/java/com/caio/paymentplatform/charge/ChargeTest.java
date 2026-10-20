package com.caio.paymentplatform.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.caio.paymentplatform.charge.exceptions.ChargeStateTransitionException;

class ChargeTest {
    
    @Test
    void shouldMarkPendingChargeAsPaid() {

        Charge charge = Charge.create(
            new BigDecimal("100.00"),
            "Test charge"
        );

        charge.markAsPaid();

        assertEquals(ChargeStatus.PAID, charge.getStatus());
    }

    @Test
    void shouldCancelPendingCharge() {

        Charge charge = Charge.create(
            new BigDecimal("100.00"),
            "Test charge"
        );

        charge.cancel();

        assertEquals(ChargeStatus.CANCELLED, charge.getStatus());
    }

    @Test
    void shouldExpirePendingCharge() {

        Charge charge = Charge.create(
            new BigDecimal("100.00"),
            "Test charge"
        );

        charge.expire();

        assertEquals(ChargeStatus.EXPIRED, charge.getStatus());
    }

    @Test
    void shouldNotAllowPaidChargeToBeCancelled() {

        Charge charge = Charge.create(
            new BigDecimal("100.00"),
            "Test charge"
        );
        charge.markAsPaid();

        assertThrows(ChargeStateTransitionException.class, charge::cancel);
    }

    @Test
    void shouldNotAllowCancelledChargeToBePaid() {

        Charge charge = Charge.create(
            new BigDecimal("100.00"),
            "Test charge"
        );
        charge.cancel();

        assertThrows(ChargeStateTransitionException.class, charge::markAsPaid);
    }

    @Test
    void shouldNotAllowExpiredChargeToBePaid() {

        Charge charge = Charge.create(
            new BigDecimal("100.00"),
            "Test charge"
        );
        charge.expire();

        assertThrows(ChargeStateTransitionException.class, charge::markAsPaid);
    }
}