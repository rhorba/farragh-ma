package ma.farragh.backend.payments;

import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.RegisterRequest;
import ma.farragh.backend.payments.dto.PaymentResponseDto;
import ma.farragh.backend.recyclers.dto.DeclareMaterialsDto;
import ma.farragh.backend.recyclers.dto.DeclareZoneDto;
import ma.farragh.backend.requests.PickupRequestRepository;
import ma.farragh.backend.requests.dto.CreateRequestDto;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class PaymentControllerTest {

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

    @BeforeEach
    void cleanUp() {
        pickupRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndGetToken(String email, Role role) {
        RegisterRequest register = new RegisterRequest(email, "a-strong-password", role, "Test User", "0600000000", "fr");
        AuthResponse response = restClient.post().uri("/api/v1/auth/register")
                .body(register)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        return response.accessToken();
    }

    private UUID completeRequestFlow(String householdToken, String recyclerToken) {
        restClient.post().uri("/api/v1/recyclers/zone").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareZoneDto(33.5731, -7.5898, 5000, null)).exchange().expectStatus().isOk();
        restClient.put().uri("/api/v1/recyclers/materials").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareMaterialsDto(java.util.List.of("PLASTIC"))).exchange().expectStatus().isOk();

        CreateRequestDto dto = new CreateRequestDto("PLASTIC", null, "Some address, Casablanca", 33.5735, -7.5890, null);
        UUID requestId = restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + householdToken)
                .body(dto)
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class).returnResult().getResponseBody().id();

        restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + recyclerToken).exchange().expectStatus().isOk();
        restClient.post().uri("/api/v1/recyclers/requests/" + requestId + "/schedule")
                .header("Authorization", "Bearer " + recyclerToken).exchange().expectStatus().isOk();
        restClient.post().uri("/api/v1/recyclers/requests/" + requestId + "/complete")
                .header("Authorization", "Bearer " + recyclerToken).exchange().expectStatus().isOk();

        return requestId;
    }

    @Test
    void payingACompletedRequestSucceeds() {
        String householdToken = registerAndGetToken("pay-household@example.com", Role.HOUSEHOLD_SME);
        String recyclerToken = registerAndGetToken("pay-recycler@example.com", Role.RECYCLER);
        UUID requestId = completeRequestFlow(householdToken, recyclerToken);

        PaymentResponseDto payment = restClient.post().uri("/api/v1/requests/" + requestId + "/payment")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(payment).isNotNull();
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.mode()).isEqualTo(PaymentMode.MOCK);
        assertThat(payment.pickupRequestId()).isEqualTo(requestId);

        RequestResponseDto detail = restClient.get().uri("/api/v1/requests/" + requestId)
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();
        assertThat(detail.paymentStatus()).isEqualTo("SUCCEEDED");
    }

    @Test
    void payingABeforeCompletedRequestIsRejected() {
        String householdToken = registerAndGetToken("early-pay-household@example.com", Role.HOUSEHOLD_SME);
        CreateRequestDto dto = new CreateRequestDto("PLASTIC", null, "Some address, Casablanca", 33.5735, -7.5890, null);
        UUID requestId = restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + householdToken)
                .body(dto)
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class).returnResult().getResponseBody().id();

        restClient.post().uri("/api/v1/requests/" + requestId + "/payment")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void doubleSubmitPaymentIsRejected() {
        String householdToken = registerAndGetToken("double-pay-household@example.com", Role.HOUSEHOLD_SME);
        String recyclerToken = registerAndGetToken("double-pay-recycler@example.com", Role.RECYCLER);
        UUID requestId = completeRequestFlow(householdToken, recyclerToken);

        restClient.post().uri("/api/v1/requests/" + requestId + "/payment")
                .header("Authorization", "Bearer " + householdToken)
                .exchange().expectStatus().isOk();

        restClient.post().uri("/api/v1/requests/" + requestId + "/payment")
                .header("Authorization", "Bearer " + householdToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void payingAnotherHouseholdsRequestReturnsNotFound() {
        String ownerToken = registerAndGetToken("pay-owner@example.com", Role.HOUSEHOLD_SME);
        String intruderToken = registerAndGetToken("pay-intruder@example.com", Role.HOUSEHOLD_SME);
        String recyclerToken = registerAndGetToken("pay-intrusion-recycler@example.com", Role.RECYCLER);
        UUID requestId = completeRequestFlow(ownerToken, recyclerToken);

        restClient.post().uri("/api/v1/requests/" + requestId + "/payment")
                .header("Authorization", "Bearer " + intruderToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }
}
