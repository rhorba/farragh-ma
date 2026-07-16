package ma.farragh.backend.auth.dto;

import ma.farragh.backend.auth.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        Role role,
        String preferredLang
) {
}
