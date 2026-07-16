package ma.farragh.backend.municipality.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionResponseDto(
        UUID id,
        Double centerLatitude,
        Double centerLongitude,
        Integer radiusM,
        List<List<Double>> polygon,
        boolean active,
        Instant createdAt
) {
}
