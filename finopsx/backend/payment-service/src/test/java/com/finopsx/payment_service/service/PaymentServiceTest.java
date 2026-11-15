package com.finopsx.payment_service.service;

import com.finopsx.payment_service.domain.Payment;
import com.finopsx.payment_service.domain.PaymentStatus;
import com.finopsx.payment_service.dto.CreatePaymentRequest;
import com.finopsx.payment_service.dto.PaymentResponse;
import com.finopsx.payment_service.event.PaymentEvent;
import com.finopsx.payment_service.exception.IdempotencyConflictException;
import com.finopsx.payment_service.exception.InvalidPaymentException;
import com.finopsx.payment_service.exception.InvalidStatusTransitionException;
import com.finopsx.payment_service.exception.PaymentNotFoundException;
import com.finopsx.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String KEY = "idem-123";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private final CreatePaymentRequest request = new CreatePaymentRequest(
            "ACC-1001", "ACC-2002", new BigDecimal("2500.00"), "INR", "Invoice 42");

    @Test
    void createsNewPaymentAndPublishesEvent() {
        when(paymentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentService.CreateResult result = paymentService.create(KEY, request);

        assertThat(result.replayed()).isFalse();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.payment().amount()).isEqualByComparingTo("2500.00");

        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo("PAYMENT_INITIATED");
        assertThat(event.getValue().paymentId()).isEqualTo(result.payment().id());
    }

    @Test
    void retryWithSameKeyAndBodyReturnsOriginalPayment() {
        Payment existing = Payment.initiate(KEY, "ACC-1001", "ACC-2002", new BigDecimal("2500.0000"), "INR", "Invoice 42");
        when(paymentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        PaymentService.CreateResult result = paymentService.create(KEY, request);

        assertThat(result.replayed()).isTrue();
        assertThat(result.payment().id()).isEqualTo(existing.getId());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void sameKeyWithDifferentBodyIsRejected() {
        Payment existing = Payment.initiate(KEY, "ACC-1001", "ACC-2002", new BigDecimal("100.00"), "INR", "Invoice 42");
        when(paymentRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.create(KEY, request))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void sameDebtorAndCreditorIsRejected() {
        CreatePaymentRequest selfTransfer = new CreatePaymentRequest(
                "ACC-1001", "ACC-1001", new BigDecimal("10.00"), "INR", null);

        assertThatThrownBy(() -> paymentService.create(KEY, selfTransfer))
                .isInstanceOf(InvalidPaymentException.class);
    }

    @Test
    void validStatusUpdateIsAppliedAndPublished() {
        Payment payment = Payment.initiate(KEY, "ACC-1001", "ACC-2002", new BigDecimal("10.00"), "INR", null);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.updateStatus(payment.getId(), PaymentStatus.PROCESSING, null);

        assertThat(response.status()).isEqualTo(PaymentStatus.PROCESSING);
        verify(eventPublisher).publishEvent(any(PaymentEvent.class));
    }

    @Test
    void failedPaymentKeepsReason() {
        Payment payment = Payment.initiate(KEY, "ACC-1001", "ACC-2002", new BigDecimal("10.00"), "INR", null);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.updateStatus(payment.getId(), PaymentStatus.FAILED, "Insufficient funds");

        assertThat(response.failureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void illegalStatusJumpIsRejected() {
        Payment payment = Payment.initiate(KEY, "ACC-1001", "ACC-2002", new BigDecimal("10.00"), "INR", null);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.updateStatus(payment.getId(), PaymentStatus.COMPLETED, null))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void unknownPaymentIsNotFound() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.get(id))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
