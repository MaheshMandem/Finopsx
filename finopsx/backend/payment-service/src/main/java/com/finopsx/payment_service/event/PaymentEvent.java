package com.finopsx.payment_service.event;

import com.finopsx.payment_service.domain.Payment;
import com.finopsx.payment_service.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Event published to Kafka whenever a payment is created or changes status. */
public record PaymentEvent(
        UUID eventId,
        String type,
        UUID paymentId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {

    public static PaymentEvent of(Payment payment) {
        return new PaymentEvent(
                UUID.randomUUID(),
                "PAYMENT_" + payment.getStatus().name(),
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                Instant.now()
        );
    }
}
