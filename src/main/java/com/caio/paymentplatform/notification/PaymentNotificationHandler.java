
package com.caio.paymentplatform.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.caio.paymentplatform.charge.events.ChargePaidEvent;

@Component
public class PaymentNotificationHandler {

    private final PaymentNotificationRepository notificationRepository;

    public PaymentNotificationHandler(PaymentNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(ChargePaidEvent event) {
        
        PaymentNotification notification = PaymentNotification.create(
            event.eventId(),
            event.chargeId(),
            "CHARGE_PAID",
            "Payment of " + event.amount() + " "
                + event.currency() + " was successfully confirmed."
        );

        notificationRepository.save(notification);
    }
}
