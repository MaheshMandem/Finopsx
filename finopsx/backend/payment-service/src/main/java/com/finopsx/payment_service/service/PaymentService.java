package com.finopsx.payment_service.service;

import com.finopsx.payment_service.domain.Payment;
import com.finopsx.payment_service.domain.PaymentStatus;
import com.finopsx.payment_service.dto.CreatePaymentRequest;
import com.finopsx.payment_service.dto.PaymentResponse;
import com.finopsx.payment_service.event.PaymentEvent;
import com.finopsx.payment_service.exception.IdempotencyConflictException;
import com.finopsx.payment_service.exception.InvalidPaymentException;
import com.finopsx.payment_service.exception.PaymentNotFoundException;
import com.finopsx.payment_service.repository.PaymentRepository;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    public static final String PAYMENTS_CACHE = "payments";

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a payment exactly once per Idempotency-Key.
     * A retry with the same key and same body returns the original payment (replayed = true);
     * the same key with a different body is rejected.
     */
    @Transactional
    public CreateResult create(String idempotencyKey, CreatePaymentRequest request) {
        if (request.debtorAccount().equals(request.creditorAccount())) {
            throw new InvalidPaymentException("Debtor and creditor accounts must be different");
        }

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (!payment.matches(request.debtorAccount(), request.creditorAccount(), request.amount(),
                    request.currency(), request.reference())) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return new CreateResult(PaymentResponse.from(payment), true);
        }

        Payment payment = Payment.initiate(idempotencyKey, request.debtorAccount(), request.creditorAccount(),
                request.amount(), request.currency(), request.reference());
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(PaymentEvent.of(saved));
        return new CreateResult(PaymentResponse.from(saved), false);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = PAYMENTS_CACHE, key = "#id")
    public PaymentResponse get(UUID id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional
    @CachePut(cacheNames = PAYMENTS_CACHE, key = "#id")
    public PaymentResponse updateStatus(UUID id, PaymentStatus target, String reason) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        payment.transitionTo(target, reason);
        eventPublisher.publishEvent(PaymentEvent.of(payment));
        return PaymentResponse.from(payment);
    }

    public record CreateResult(PaymentResponse payment, boolean replayed) {
    }
}
