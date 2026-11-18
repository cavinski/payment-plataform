
package com.caio.paymentplatform.charge.outbox;

import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock
    private OutboxProcessor outboxProcessor;
    
    @Mock
    private OutboxRetryService outboxRetryService;

    @InjectMocks
    private OutboxWorker outboxWorker;

    @Test
    void shouldProcessEventsUntilNoPendingEventsRemain() {

        when(outboxProcessor.processNext())
            .thenReturn(true)
            .thenReturn(true)
            .thenReturn(true)
            .thenReturn(false);

        outboxWorker.processPendingEvents();

        verify(outboxProcessor, times(4)).processNext();
    }

    @Test
    void shouldStopImmediatelyWhenNoPendingEventsExist() {
        
        when(outboxProcessor.processNext()).thenReturn(false);

        outboxWorker.processPendingEvents();

        verify(outboxProcessor, times(1)).processNext();
    }

    @Test
    void shouldContinueProcessingAfterEventFailure() {
        
        UUID eventId = UUID.randomUUID();
        RuntimeException cause =
            new RuntimeException("Notification service unavailable");

        OutboxProcessingException exception =
            new OutboxProcessingException(eventId, cause);

        when(outboxProcessor.processNext())
            .thenThrow(exception)
            .thenReturn(true)
            .thenReturn(false);

        outboxWorker.processPendingEvents();

        verify(outboxRetryService).recordFailure(eventId, cause);
        verify(outboxProcessor, times(3)).processNext();
    }
}
