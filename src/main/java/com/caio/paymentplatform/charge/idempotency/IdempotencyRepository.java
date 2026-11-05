package com.caio.paymentplatform.charge.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID>{
    
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
        INSERT INTO idempotency_records (
            id,
            idempotency_key,
            request_hash,
            created_at
        )
        VALUES (
            :id,
            :idempotencyKey,
            :requestHash,
            :createdAt
        )
        ON CONFLICT (idempotency_key) DO NOTHING
        """, nativeQuery = true)
        
    int insertIfAbsent(
        @Param("id") UUID id,
        @Param("idempotencyKey") String idempotencyKey,
        @Param("requestHash") String requestHash,
        @Param("createdAt") Instant createdAt
    );
}