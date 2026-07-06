package ma.farragh.backend.recyclers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeclareMaterialsDto(
        @NotEmpty List<@NotBlank String> materialTypeCodes
) {
}
