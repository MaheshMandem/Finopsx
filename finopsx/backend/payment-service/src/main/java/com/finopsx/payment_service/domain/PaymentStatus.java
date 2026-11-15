package com.finopsx.payment_service.domain;

/**
 * Lifecycle of a payment. Transitions are explicit so a payment can never
 * jump from INITIATED straight to COMPLETED or be modified once it is final.
 */
public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case INITIATED -> target == PROCESSING || target == CANCELLED || target == FAILED;
            case PROCESSING -> target == COMPLETED || target == FAILED;
            case COMPLETED, FAILED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
