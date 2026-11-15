package com.finopsx.payment_service.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        List<String> details,
        Instant timestamp
) {
}
