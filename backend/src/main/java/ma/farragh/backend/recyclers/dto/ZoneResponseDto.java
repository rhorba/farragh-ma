package ma.farragh.backend.recyclers.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ZoneResponseDto(
        UUID id,
        Double centerLatitude,
        Double centerLongitude,
        Integer radiusM,
        List<List<Double>> polygon,
        Instant createdAt
) {
}
