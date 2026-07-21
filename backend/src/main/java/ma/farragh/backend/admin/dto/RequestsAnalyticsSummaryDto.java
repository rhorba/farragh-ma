package ma.farragh.backend.admin.dto;

import ma.farragh.backend.requests.RequestStatus;

import java.time.Instant;
import java.util.Map;

public record RequestsAnalyticsSummaryDto(
        Instant from,
        Instant to,
        long total,
        Map<RequestStatus, Long> countsByStatus
) {
}
