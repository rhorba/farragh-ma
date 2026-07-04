package ma.farragh.backend.auth;

import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.LoginRequest;
import ma.farragh.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AuthControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4")
                    .asCompatibleSubstituteFor("postgres"));

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private RegisterRequest sampleHouseholdRequest(String email) {
        return new RegisterRequest(email, "a-strong-password", Role.HOUSEHOLD_SME, "Amina Test", "0600000000", "fr");
    }

    @Test
    void registerCreatesUserWithHashedPasswordAndReturnsTokens() {
        AuthResponse body = restClient.post().uri("/api/v1/auth/register")
                .body(sampleHouseholdRequest("amina@example.com"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.role()).isEqualTo(Role.HOUSEHOLD_SME);

        User stored = userRepository.findByEmail("amina@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("a-strong-password");
    }

    @Test
    void registerWithDuplicateEmailIsRejected() {
        restClient.post().uri("/api/v1/auth/register")
                .body(sampleHouseholdRequest("dup@example.com"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED);

        restClient.post().uri("/api/v1/auth/register")
                .body(sampleHouseholdRequest("dup@example.com"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginWithValidCredentialsReturnsTokenWithRoleClaim() {
        restClient.post().uri("/api/v1/auth/register")
                .body(sampleHouseholdRequest("login@example.com"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED);

        AuthResponse body = restClient.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("login@example.com", "a-strong-password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.role()).isEqualTo(Role.HOUSEHOLD_SME);
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        restClient.post().uri("/api/v1/auth/register")
                .body(sampleHouseholdRequest("wrongpass@example.com"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED);

        restClient.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("wrongpass@example.com", "totally-wrong"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void fifthFailedLoginLocksTheAccount() {
        restClient.post().uri("/api/v1/auth/register")
                .body(sampleHouseholdRequest("lockout@example.com"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED);

        LoginRequest badLogin = new LoginRequest("lockout@example.com", "totally-wrong");
        for (int i = 0; i < 5; i++) {
            restClient.post().uri("/api/v1/auth/login").body(badLogin).exchange();
        }

        restClient.post().uri("/api/v1/auth/login")
                .body(badLogin)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
