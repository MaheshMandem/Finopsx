package com.finopsx.payment_service.dto;

import com.finopsx.payment_service.domain.Payment;
import com.finopsx.payment_service.domain.PaymentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** API view of a payment. Serializable so it can be cached in Redis. */
public record PaymentResponse(
        UUID id,
        String debtorAccount,
        String creditorAccount,
        BigDecimal amount,
        String currency,
        String reference,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getDebtorAccount(),
                payment.getCreditorAccount(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
