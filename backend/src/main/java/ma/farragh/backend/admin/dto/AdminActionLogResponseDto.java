package ma.farragh.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminActionLogResponseDto(
        UUID id,
        String adminEmail,
        String targetEmail,
        String action,
        Instant createdAt
) {
}
