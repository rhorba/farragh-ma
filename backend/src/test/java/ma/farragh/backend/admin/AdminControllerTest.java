package ma.farragh.backend.admin;

import ma.farragh.backend.admin.dto.AdminUserResponseDto;
import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.LoginRequest;
import ma.farragh.backend.auth.dto.RegisterRequest;
import ma.farragh.backend.requests.PickupRequestRepository;
import ma.farragh.backend.requests.dto.CreateRequestDto;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AdminControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4")
                    .asCompatibleSubstituteFor("postgres"));

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanUp() {
        pickupRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String createAdminAndGetToken(String email) {
        User admin = new User(email, "not-used-in-this-test", Role.ADMIN, "Admin Test", null, "fr");
        userRepository.save(admin);
        return jwtService.generateAccessToken(admin.getId(), Role.ADMIN);
    }

    private String registerAndGetToken(String email, Role role) {
        return registerAndGetResponse(email, role).accessToken();
    }

    private AuthResponse registerAndGetResponse(String email, Role role) {
        RegisterRequest register = new RegisterRequest(email, "a-strong-password", role, "Test User", "0600000000", "fr");
        return restClient.post().uri("/api/v1/auth/register")
                .body(register)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
    }

    @Test
    void publicRegistrationAsAdminIsRejected() {
        RegisterRequest register = new RegisterRequest("wannabe-admin@example.com", "a-strong-password",
                Role.ADMIN, "Wannabe Admin", null, "fr");

        restClient.post().uri("/api/v1/auth/register")
                .body(register)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(userRepository.findByEmail("wannabe-admin@example.com")).isEmpty();
    }

    @Test
    void adminCanSearchUsersByEmailAndRole() {
        String adminToken = createAdminAndGetToken("search-admin@example.com");
        registerAndGetToken("filter-household@example.com", Role.HOUSEHOLD_SME);
        registerAndGetToken("filter-recycler@example.com", Role.RECYCLER);

        Map<String, Object> byEmail = restClient.get().uri("/api/v1/admin/users?email=filter-household")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        @SuppressWarnings("unchecked")
        var content = (java.util.List<Map<String, Object>>) byEmail.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("email")).isEqualTo("filter-household@example.com");
    }

    @Test
    void adminCanSearchUsersWithNoFilters() {
        String adminToken = createAdminAndGetToken("nofilter-admin@example.com");
        registerAndGetToken("nofilter-household@example.com", Role.HOUSEHOLD_SME);

        Map<String, Object> all = restClient.get().uri("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        @SuppressWarnings("unchecked")
        var content = (java.util.List<Map<String, Object>>) all.get("content");
        assertThat(content).hasSize(2);
    }

    @Test
    void nonAdminCannotCallAdminUserSearch() {
        String householdToken = registerAndGetToken("intruder-household@example.com", Role.HOUSEHOLD_SME);

        restClient.get().uri("/api/v1/admin/users")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanSearchRequestsByStatus() {
        String adminToken = createAdminAndGetToken("requests-admin@example.com");
        String householdToken = registerAndGetToken("requests-household@example.com", Role.HOUSEHOLD_SME);
        CreateRequestDto dto = new CreateRequestDto("PLASTIC", null, "Some address, Casablanca", 33.5731, -7.5898, null);
        restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + householdToken)
                .body(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();

        Map<String, Object> posted = restClient.get().uri("/api/v1/admin/requests?status=POSTED")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        @SuppressWarnings("unchecked")
        var content = (java.util.List<Map<String, Object>>) posted.get("content");
        assertThat(content).hasSize(1);

        Map<String, Object> completed = restClient.get().uri("/api/v1/admin/requests?status=COMPLETED")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        @SuppressWarnings("unchecked")
        var completedContent = (java.util.List<Map<String, Object>>) completed.get("content");
        assertThat(completedContent).isEmpty();
    }

    @Test
    void nonAdminCannotCallAdminRequestSearch() {
        String recyclerToken = registerAndGetToken("intruder-recycler@example.com", Role.RECYCLER);

        restClient.get().uri("/api/v1/admin/requests")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanDeactivateUserAndSubsequentLoginFails() {
        String adminToken = createAdminAndGetToken("deactivate-admin@example.com");
        AuthResponse target = registerAndGetResponse("deactivate-target@example.com", Role.HOUSEHOLD_SME);

        restClient.post().uri("/api/v1/admin/users/" + target.userId() + "/deactivate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AdminUserResponseDto.class)
                .value(dto -> assertThat(dto.active()).isFalse());

        restClient.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("deactivate-target@example.com", "a-strong-password"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCannotDeactivateOwnAccount() {
        User admin = new User("self-deactivate-admin@example.com", "not-used-in-this-test", Role.ADMIN, "Admin Test", null, "fr");
        userRepository.save(admin);
        String adminToken = jwtService.generateAccessToken(admin.getId(), Role.ADMIN);

        restClient.post().uri("/api/v1/admin/users/" + admin.getId() + "/deactivate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deactivatingUnknownUserReturnsNotFound() {
        String adminToken = createAdminAndGetToken("notfound-admin@example.com");

        restClient.post().uri("/api/v1/admin/users/" + java.util.UUID.randomUUID() + "/deactivate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminCanReactivateADeactivatedUser() {
        String adminToken = createAdminAndGetToken("reactivate-admin@example.com");
        AuthResponse target = registerAndGetResponse("reactivate-target@example.com", Role.HOUSEHOLD_SME);

        restClient.post().uri("/api/v1/admin/users/" + target.userId() + "/deactivate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();

        restClient.post().uri("/api/v1/admin/users/" + target.userId() + "/reactivate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AdminUserResponseDto.class)
                .value(dto -> assertThat(dto.active()).isTrue());

        restClient.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("reactivate-target@example.com", "a-strong-password"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actionLogRecordsDeactivateAndReactivateInOrder() {
        String adminToken = createAdminAndGetToken("actionlog-admin@example.com");
        AuthResponse target = registerAndGetResponse("actionlog-target@example.com", Role.HOUSEHOLD_SME);

        restClient.post().uri("/api/v1/admin/users/" + target.userId() + "/deactivate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange().expectStatus().isOk();
        restClient.post().uri("/api/v1/admin/users/" + target.userId() + "/reactivate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange().expectStatus().isOk();

        Map<String, Object> log = restClient.get().uri("/api/v1/admin/action-log")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        @SuppressWarnings("unchecked")
        var content = (java.util.List<Map<String, Object>>) log.get("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).get("action")).isEqualTo("REACTIVATE");
        assertThat(content.get(1).get("action")).isEqualTo("DEACTIVATE");
        assertThat(content.get(0).get("targetEmail")).isEqualTo("actionlog-target@example.com");
    }

    @Test
    void nonAdminCannotManageUserActivationOrViewActionLog() {
        String householdToken = registerAndGetToken("intruder-activation@example.com", Role.HOUSEHOLD_SME);
        java.util.UUID someId = java.util.UUID.randomUUID();

        restClient.post().uri("/api/v1/admin/users/" + someId + "/deactivate")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

        restClient.post().uri("/api/v1/admin/users/" + someId + "/reactivate")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

        restClient.get().uri("/api/v1/admin/action-log")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }
}
