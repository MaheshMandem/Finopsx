package com.finopsx.payment_service.web;

import com.finopsx.payment_service.dto.ApiError;
import com.finopsx.payment_service.exception.IdempotencyConflictException;
import com.finopsx.payment_service.exception.InvalidPaymentException;
import com.finopsx.payment_service.exception.InvalidStatusTransitionException;
import com.finopsx.payment_service.exception.PaymentNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(PaymentNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler({IdempotencyConflictException.class, InvalidStatusTransitionException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDuplicate(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "A request with this Idempotency-Key is already being processed", List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "Payment was updated concurrently, please retry", List.of());
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ApiError> handleInvalidPayment(InvalidPaymentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(HttpStatus.BAD_REQUEST, "Missing required header: " + ex.getHeaderName(), List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleUnreadable(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request", List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message, details, Instant.now());
        return ResponseEntity.status(status).body(body);
    }
}
