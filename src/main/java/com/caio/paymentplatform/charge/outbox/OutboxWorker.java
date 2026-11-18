
package com.caio.paymentplatform.charge.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {

    private final OutboxProcessor outboxProcessor;
    private final OutboxRetryService outboxRetryService;

    public OutboxWorker(
        OutboxProcessor outboxProcessor,
        OutboxRetryService outboxRetryService
    ) {
        this.outboxProcessor = outboxProcessor;
        this.outboxRetryService = outboxRetryService;
    }

    @Scheduled(fixedDelayString = "${outbox.worker.fixed-delay:1000}")
    public void processPendingEvents() {
        while (true) {
            try {
                if (!outboxProcessor.processNext()) {
                    break;
                }
            } catch (OutboxProcessingException exception) {
                outboxRetryService.recordFailure(
                    exception.getEventId(),
                    exception.getCause()
                );
            }
        }
    }
}
