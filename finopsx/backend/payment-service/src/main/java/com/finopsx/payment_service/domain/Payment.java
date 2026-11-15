package com.finopsx.payment_service.domain;

import com.finopsx.payment_service.exception.InvalidStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotency_key")
)
public class Payment {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "debtor_account", nullable = false, length = 34)
    private String debtorAccount;

    @Column(name = "creditor_account", nullable = false, length = 34)
    private String creditorAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 140)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    /** Optimistic locking: concurrent status updates cannot silently overwrite each other. */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
        // required by JPA
    }

    public static Payment initiate(String idempotencyKey, String debtorAccount, String creditorAccount,
                                   BigDecimal amount, String currency, String reference) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.idempotencyKey = idempotencyKey;
        payment.debtorAccount = debtorAccount;
        payment.creditorAccount = creditorAccount;
        payment.amount = amount;
        payment.currency = currency;
        payment.reference = reference;
        payment.status = PaymentStatus.INITIATED;
        payment.createdAt = Instant.now();
        payment.updatedAt = payment.createdAt;
        return payment;
    }

    /** True when a retried request carries exactly the same payment instruction. */
    public boolean matches(String debtorAccount, String creditorAccount, BigDecimal amount,
                           String currency, String reference) {
        return this.debtorAccount.equals(debtorAccount)
                && this.creditorAccount.equals(creditorAccount)
                && this.amount.compareTo(amount) == 0
                && this.currency.equals(currency)
                && Objects.equals(this.reference, reference);
    }

    public void transitionTo(PaymentStatus target, String reason) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(id, status, target);
        }
        this.status = target;
        this.failureReason = target == PaymentStatus.FAILED ? reason : null;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDebtorAccount() {
        return debtorAccount;
    }

    public String getCreditorAccount() {
        return creditorAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReference() {
        return reference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
