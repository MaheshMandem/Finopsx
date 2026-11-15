package com.finopsx.payment_service.web;

import com.finopsx.payment_service.config.SecurityConfig;
import com.finopsx.payment_service.domain.PaymentStatus;
import com.finopsx.payment_service.dto.PaymentResponse;
import com.finopsx.payment_service.exception.IdempotencyConflictException;
import com.finopsx.payment_service.exception.PaymentNotFoundException;
import com.finopsx.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    private static final String VALID_BODY = """
            {"debtorAccount":"ACC-1001","creditorAccount":"ACC-2002","amount":2500.00,"currency":"INR","reference":"Invoice 42"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private PaymentResponse samplePayment() {
        Instant now = Instant.now();
        return new PaymentResponse(UUID.randomUUID(), "ACC-1001", "ACC-2002", new BigDecimal("2500.00"),
                "INR", "Invoice 42", PaymentStatus.INITIATED, null, now, now);
    }

    @Test
    @WithMockUser
    void createReturns201WithLocation() throws Exception {
        PaymentResponse payment = samplePayment();
        when(paymentService.create(eq("key-1"), any())).thenReturn(new PaymentService.CreateResult(payment, false));

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/" + payment.id()))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @WithMockUser
    void replayedRequestReturns200() throws Exception {
        when(paymentService.create(eq("key-1"), any())).thenReturn(new PaymentService.CreateResult(samplePayment(), true));

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void missingIdempotencyKeyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required header: Idempotency-Key"));
    }

    @Test
    @WithMockUser
    void invalidBodyReturns400WithDetails() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"debtorAccount":"","creditorAccount":"ACC-2002","amount":-5,"currency":"rupees"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.length()").value(3));
    }

    @Test
    @WithMockUser
    void idempotencyConflictReturns409() throws Exception {
        when(paymentService.create(eq("key-1"), any())).thenThrow(new IdempotencyConflictException("key-1"));

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void unknownPaymentReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.get(id)).thenThrow(new PaymentNotFoundException(id));

        mockMvc.perform(get("/api/v1/payments/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
