package ma.farragh.backend.municipality.dto;

import jakarta.validation.constraints.Positive;

import java.util.List;

public record SubscribeZoneDto(
        Double centerLatitude,
        Double centerLongitude,
        @Positive Integer radiusM,
        List<List<Double>> polygon,
        boolean confirmOverlap
) {
}
