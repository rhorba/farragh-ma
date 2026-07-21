package ma.farragh.backend.admin.dto;

import java.time.Instant;

public record RequestsTimeSeriesPointDto(
        Instant bucket,
        long created,
        long completed
) {
}
