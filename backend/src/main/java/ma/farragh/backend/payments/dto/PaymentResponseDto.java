package ma.farragh.backend.payments.dto;

import ma.farragh.backend.payments.PaymentMode;
import ma.farragh.backend.payments.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponseDto(
        UUID id,
        UUID pickupRequestId,
        int amountCents,
        String currency,
        String provider,
        PaymentMode mode,
        PaymentStatus status,
        String providerRef,
        Instant createdAt
) {
}
