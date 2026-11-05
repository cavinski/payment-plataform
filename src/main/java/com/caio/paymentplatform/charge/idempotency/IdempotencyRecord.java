package com.caio.paymentplatform.charge.idempotency;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity 
@Table(
    name = "idempotency_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_records_key",
        columnNames = "idempotency_key"
    ) 
)
public class IdempotencyRecord {
    
    @Id 
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected IdempotencyRecord() {

    }

    private IdempotencyRecord(String idempotencyKey, String requestHash) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
    }

    public static IdempotencyRecord create(String idempotencyKey, String requestHash) {
        return new IdempotencyRecord(idempotencyKey, requestHash);
    }

    public void complete(int responseStatus, String responseBody) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.completedAt = Instant.now();
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }
}