package com.finopsx.payment_service.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTest {

    @Test
    void initiatedCanMoveToProcessingCancelledOrFailed() {
        assertThat(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.PROCESSING)).isTrue();
        assertThat(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.CANCELLED)).isTrue();
        assertThat(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.FAILED)).isTrue();
        assertThat(PaymentStatus.INITIATED.canTransitionTo(PaymentStatus.COMPLETED)).isFalse();
    }

    @Test
    void processingCanOnlyCompleteOrFail() {
        assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.COMPLETED)).isTrue();
        assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.FAILED)).isTrue();
        assertThat(PaymentStatus.PROCESSING.canTransitionTo(PaymentStatus.CANCELLED)).isFalse();
    }

    @Test
    void terminalStatusesCannotChange() {
        for (PaymentStatus terminal : new PaymentStatus[]{PaymentStatus.COMPLETED, PaymentStatus.FAILED, PaymentStatus.CANCELLED}) {
            assertThat(terminal.isTerminal()).isTrue();
            for (PaymentStatus target : PaymentStatus.values()) {
                assertThat(terminal.canTransitionTo(target)).isFalse();
            }
        }
    }
}
