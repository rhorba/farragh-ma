package ma.farragh.backend.requests.dto;

import ma.farragh.backend.requests.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record RequestResponseDto(
        UUID id,
        String materialTypeCode,
        String quantityDesc,
        String addressText,
        double latitude,
        double longitude,
        RequestStatus status,
        String photoUrl,
        Instant createdAt,
        Instant updatedAt,
        String paymentStatus
) {
}
