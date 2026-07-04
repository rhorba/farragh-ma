package ma.farragh.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ma.farragh.backend.auth.Role;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, message = "Password must be at least 10 characters") String password,
        @NotNull Role role,
        @NotBlank String fullName,
        String phone,
        @Pattern(regexp = "fr|ar", message = "preferredLang must be 'fr' or 'ar'") String preferredLang
) {
}
