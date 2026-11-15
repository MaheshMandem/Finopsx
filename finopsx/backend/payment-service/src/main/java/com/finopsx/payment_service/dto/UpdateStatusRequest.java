package com.finopsx.payment_service.dto;

import com.finopsx.payment_service.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotNull PaymentStatus status,
        @Size(max = 255) String reason
) {
}
