package ma.farragh.backend.admin.dto;

import ma.farragh.backend.auth.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponseDto(
        UUID id,
        String email,
        Role role,
        String fullName,
        String phone,
        boolean active,
        Instant createdAt
) {
}
