package com.caio.paymentplatform.charge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.caio.paymentplatform.charge.exceptions.ChargeStateTransitionException;
import jakarta.persistence.Version;

@Entity 
@Table(name = "charges")
public class Charge {
    
    @Id 
    private UUID id;

    @Version 
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Charge() {

    }

    private Charge(UUID id, BigDecimal amount, String description) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Charge amount must be greater than zero");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Charge amount cannot have more than two decimal places");
        }

        this.id = id;
        this.amount = amount;
        this.currency = "BRL";
        this.description = description;
        this.status = ChargeStatus.PENDING;
    }

    public static Charge create(BigDecimal amount, String description) {
        return new Charge(UUID.randomUUID(), amount, description);
    }

    public void markAsPaid() {
        transitionTo(ChargeStatus.PAID);
    }

    public void cancel() {
        transitionTo(ChargeStatus.CANCELLED);
    }

    public void expire() {
        transitionTo(ChargeStatus.EXPIRED);
    }

    private void transitionTo(ChargeStatus newStatus) {
        if (this.status != ChargeStatus.PENDING) {
            throw new ChargeStateTransitionException(this.status);
        }
        
        this.status = newStatus;
    }

    @PrePersist 
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate 
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}