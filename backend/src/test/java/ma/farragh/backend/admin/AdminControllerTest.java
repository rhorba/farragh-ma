package ma.farragh.backend.admin;

import ma.farragh.backend.admin.dto.AdminUserResponseDto;
import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.LoginRequest;
import ma.farragh.backend.auth.dto.RegisterRequest;
import ma.farragh.backend.requests.PickupRequestRepository;
import ma.farragh.backend.requests.RequestStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        pickupRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String createRequestBackdated(String householdToken, RequestStatus status, Instant createdAt) {
        CreateRequestDto dto = new CreateRequestDto("PLASTIC", null, "Some address, Casablanca", 33.5731, -7.5898, null);
        RequestResponseDto created = restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + householdToken)
                .body(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();
        jdbcTemplate.update("UPDATE pickup_requests SET status = ?, created_at = ?, updated_at = ? WHERE id = ?",
                status.name(), java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt), created.id());
        return created.id().toString();
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

    @Test
    void adminCanFilterRequestsByCreatedDateRange() {
        String adminToken = createAdminAndGetToken("daterange-admin@example.com");
        String householdToken = registerAndGetToken("daterange-household@example.com", Role.HOUSEHOLD_SME);
        Instant now = Instant.now();
        createRequestBackdated(householdToken, RequestStatus.POSTED, now.minus(60, ChronoUnit.DAYS));
        createRequestBackdated(householdToken, RequestStatus.POSTED, now.minus(1, ChronoUnit.DAYS));

        Map<String, Object> recent = restClient.get().uri("/api/v1/admin/requests?createdFrom="
                        + now.minus(5, ChronoUnit.DAYS) + "&createdTo=" + now.plus(1, ChronoUnit.DAYS))
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) recent.get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void requestsAnalyticsSummaryGroupsByStatus() {
        String adminToken = createAdminAndGetToken("summary-admin@example.com");
        String householdToken = registerAndGetToken("summary-household@example.com", Role.HOUSEHOLD_SME);
        Instant now = Instant.now();
        createRequestBackdated(householdToken, RequestStatus.POSTED, now.minus(2, ChronoUnit.DAYS));
        createRequestBackdated(householdToken, RequestStatus.COMPLETED, now.minus(1, ChronoUnit.DAYS));
        createRequestBackdated(householdToken, RequestStatus.COMPLETED, now.minus(1, ChronoUnit.DAYS));

        Map<String, Object> summary = restClient.get().uri("/api/v1/admin/analytics/requests/summary")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        assertThat(summary.get("total")).isEqualTo(3);
        @SuppressWarnings("unchecked")
        var countsByStatus = (Map<String, Object>) summary.get("countsByStatus");
        assertThat(countsByStatus.get("POSTED")).isEqualTo(1);
        assertThat(countsByStatus.get("COMPLETED")).isEqualTo(2);
        assertThat(countsByStatus.get("CANCELLED")).isEqualTo(0);
    }

    @Test
    void requestsAnalyticsTimeSeriesGroupsByDayBucket() {
        String adminToken = createAdminAndGetToken("timeseries-admin@example.com");
        String householdToken = registerAndGetToken("timeseries-household@example.com", Role.HOUSEHOLD_SME);
        Instant now = Instant.now();
        Instant dayOneStart = now.truncatedTo(ChronoUnit.DAYS).minus(3, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS);
        Instant dayTwoStart = now.truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS);
        createRequestBackdated(householdToken, RequestStatus.POSTED, dayOneStart);
        createRequestBackdated(householdToken, RequestStatus.POSTED, dayOneStart.plus(1, ChronoUnit.HOURS));
        createRequestBackdated(householdToken, RequestStatus.COMPLETED, dayTwoStart);

        List<Map<String, Object>> points = restClient.get().uri("/api/v1/admin/analytics/requests/timeseries?granularity=DAY")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult().getResponseBody();

        assertThat(points).hasSize(2);
        assertThat(points.get(0).get("created")).isEqualTo(2);
        assertThat(points.get(0).get("completed")).isEqualTo(0);
        // day-two's COMPLETED request also counts toward "created" for its own bucket, not just "completed"
        assertThat(points.get(1).get("created")).isEqualTo(1);
        assertThat(points.get(1).get("completed")).isEqualTo(1);
    }

    @Test
    void requestsAnalyticsExportReturnsCsv() {
        String adminToken = createAdminAndGetToken("export-admin@example.com");
        String householdToken = registerAndGetToken("export-household@example.com", Role.HOUSEHOLD_SME);
        createRequestBackdated(householdToken, RequestStatus.POSTED, Instant.now().minus(1, ChronoUnit.DAYS));

        String csv = restClient.get().uri("/api/v1/admin/analytics/requests/export?granularity=DAY")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv")
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(csv).startsWith("bucket,created,completed\n");
        assertThat(csv.lines().count()).isEqualTo(2);
    }

    @Test
    void analyticsRejectsFromAfterTo() {
        String adminToken = createAdminAndGetToken("badrange-admin@example.com");
        Instant now = Instant.now();

        restClient.get().uri("/api/v1/admin/analytics/requests/summary?from=" + now + "&to=" + now.minus(1, ChronoUnit.DAYS))
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void malformedDateParamReturnsBadRequestNotInternalError() {
        String adminToken = createAdminAndGetToken("malformed-date-admin@example.com");

        restClient.get().uri("/api/v1/admin/analytics/requests/summary?from=not-a-date")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void malformedStatusParamReturnsBadRequestNotInternalError() {
        String adminToken = createAdminAndGetToken("malformed-status-admin@example.com");

        restClient.get().uri("/api/v1/admin/requests?status=NOT_A_REAL_STATUS")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void nonAdminCannotCallAnalyticsEndpoints() {
        String householdToken = registerAndGetToken("intruder-analytics@example.com", Role.HOUSEHOLD_SME);

        restClient.get().uri("/api/v1/admin/analytics/requests/summary")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

        restClient.get().uri("/api/v1/admin/analytics/requests/timeseries")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);

        restClient.get().uri("/api/v1/admin/analytics/requests/export")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }
}
