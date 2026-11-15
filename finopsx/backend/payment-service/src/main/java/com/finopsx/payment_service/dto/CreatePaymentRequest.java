package com.finopsx.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank @Size(max = 34) String debtorAccount,
        @NotBlank @Size(max = 34) String creditorAccount,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO 4217 code") String currency,
        @Size(max = 140) String reference
) {
}
