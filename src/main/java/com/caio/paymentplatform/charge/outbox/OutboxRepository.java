package com.caio.paymentplatform.charge.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID>{
    
    List<OutboxEvent> findByProcessedAtIsNullOrderByCreatedAtAsc();

    @Query(value = """
        SELECT *
        FROM outbox_events
        WHERE processed_at IS NULL
        AND failed_at IS NULL
        AND next_attempt_at <= CURRENT_TIMESTAMP
        ORDER BY created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<OutboxEvent> findNextPendingForUpdate();

    @Query(value = """
        SELECT *
        FROM outbox_events
        WHERE id = :eventId
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<OutboxEvent> findByIdForUpdateSkipLocked(
        @Param("eventId") UUID eventId
    );
}