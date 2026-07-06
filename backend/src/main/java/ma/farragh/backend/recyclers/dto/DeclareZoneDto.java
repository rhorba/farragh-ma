package ma.farragh.backend.recyclers.dto;

import jakarta.validation.constraints.Positive;

import java.util.List;

public record DeclareZoneDto(
        Double centerLatitude,
        Double centerLongitude,
        @Positive Integer radiusM,
        List<List<Double>> polygon
) {
}
