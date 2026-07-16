package ma.farragh.backend.auth;

import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.LoginRequest;
import ma.farragh.backend.auth.dto.RegisterRequest;
import ma.farragh.backend.shared.exception.BusinessException;
import ma.farragh.backend.shared.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            // Security doc §"Elevation of Privilege": roles are set server-side, never client-supplied.
            // ADMIN is deliberately excluded from the public register form; enforce it here too, since
            // the DTO itself accepts any Role and a direct API call could otherwise self-elevate.
            throw new BusinessException(HttpStatus.FORBIDDEN, "ADMIN_SELF_REGISTRATION_NOT_ALLOWED",
                    "The ADMIN role cannot be self-registered.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists.");
        }
        String lang = request.preferredLang() != null ? request.preferredLang() : "fr";
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role(),
                request.fullName(),
                request.phone(),
                lang);
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (loginAttemptService.isLocked(request.email())) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "ACCOUNT_LOCKED",
                    "Too many failed login attempts. Try again later.");
        }

        User user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElse(null);

        if (user == null) {
            loginAttemptService.recordFailure(request.email());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }
        if (!user.isActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED", "This account has been deactivated.");
        }

        loginAttemptService.recordSuccess(request.email());
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getRole());
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getRole(), user.getPreferredLang());
    }
}
