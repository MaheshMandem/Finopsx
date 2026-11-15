package com.finopsx.payment_service.web;

import com.finopsx.payment_service.dto.CreatePaymentRequest;
import com.finopsx.payment_service.dto.PaymentResponse;
import com.finopsx.payment_service.dto.UpdateStatusRequest;
import com.finopsx.payment_service.exception.InvalidPaymentException;
import com.finopsx.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final int MAX_KEY_LENGTH = 100;

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
                                                  @Valid @RequestBody CreatePaymentRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new InvalidPaymentException("Idempotency-Key must be 1-" + MAX_KEY_LENGTH + " characters");
        }
        PaymentService.CreateResult result = paymentService.create(idempotencyKey, request);
        if (result.replayed()) {
            return ResponseEntity.ok(result.payment());
        }
        return ResponseEntity
                .created(URI.create("/api/v1/payments/" + result.payment().id()))
                .body(result.payment());
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable("id") UUID id) {
        return paymentService.get(id);
    }

    @PatchMapping("/{id}/status")
    public PaymentResponse updateStatus(@PathVariable("id") UUID id,
                                        @Valid @RequestBody UpdateStatusRequest request) {
        return paymentService.updateStatus(id, request.status(), request.reason());
    }
}
