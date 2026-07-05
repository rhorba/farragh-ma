package ma.farragh.backend.requests.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRequestDto(
        @NotBlank String materialTypeCode,
        String quantityDesc,
        @NotBlank String addressText,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String photoUrl
) {
}
