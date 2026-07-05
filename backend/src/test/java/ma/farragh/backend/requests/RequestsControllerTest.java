package ma.farragh.backend.requests;

import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.RegisterRequest;
import ma.farragh.backend.requests.dto.CreateRequestDto;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class RequestsControllerTest {

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

    private String registerHouseholdAndGetToken(String email) {
        RegisterRequest register = new RegisterRequest(email, "a-strong-password", Role.HOUSEHOLD_SME, "Amina Test", "0600000000", "fr");
        AuthResponse response = restClient.post().uri("/api/v1/auth/register")
                .body(register)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        return response.accessToken();
    }

    private CreateRequestDto sampleRequest() {
        return new CreateRequestDto("PLASTIC", "2 bags", "12 Rue Example, Casablanca", 33.5731, -7.5898, null);
    }

    @Test
    void createRequestPersistsWithPostedStatusAndLocation() {
        String token = registerHouseholdAndGetToken("amina@example.com");

        RequestResponseDto body = restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + token)
                .body(sampleRequest())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(RequestStatus.POSTED);
        assertThat(body.materialTypeCode()).isEqualTo("PLASTIC");
        assertThat(body.latitude()).isEqualTo(33.5731);
        assertThat(body.longitude()).isEqualTo(-7.5898);
    }

    @Test
    void createRequestWithMissingAddressIsRejected() {
        String token = registerHouseholdAndGetToken("badreq@example.com");
        CreateRequestDto invalid = new CreateRequestDto("PLASTIC", "2 bags", "", 33.5731, -7.5898, null);

        restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + token)
                .body(invalid)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unauthenticatedCreateIsRejected() {
        restClient.post().uri("/api/v1/requests")
                .body(sampleRequest())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listMineOnlyReturnsOwnRequests() {
        String tokenA = registerHouseholdAndGetToken("userA@example.com");
        String tokenB = registerHouseholdAndGetToken("userB@example.com");

        restClient.post().uri("/api/v1/requests").header("Authorization", "Bearer " + tokenA)
                .body(sampleRequest()).exchange().expectStatus().isEqualTo(HttpStatus.CREATED);
        restClient.post().uri("/api/v1/requests").header("Authorization", "Bearer " + tokenB)
                .body(sampleRequest()).exchange().expectStatus().isEqualTo(HttpStatus.CREATED);

        List<RequestResponseDto> mine = restClient.get().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + tokenA)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new org.springframework.core.ParameterizedTypeReference<List<RequestResponseDto>>() {})
                .returnResult().getResponseBody();

        assertThat(mine).hasSize(1);
    }

    @Test
    void gettingAnotherUsersRequestReturnsNotFound() {
        String tokenA = registerHouseholdAndGetToken("owner@example.com");
        String tokenB = registerHouseholdAndGetToken("intruder@example.com");

        RequestResponseDto created = restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + tokenA)
                .body(sampleRequest())
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class).returnResult().getResponseBody();

        restClient.get().uri("/api/v1/requests/" + created.id())
                .header("Authorization", "Bearer " + tokenB)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelPostedRequestSucceeds() {
        String token = registerHouseholdAndGetToken("canceller@example.com");
        RequestResponseDto created = restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + token)
                .body(sampleRequest())
                .exchange().expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class).returnResult().getResponseBody();

        RequestResponseDto cancelled = restClient.post().uri("/api/v1/requests/" + created.id() + "/cancel")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(cancelled.status()).isEqualTo(RequestStatus.CANCELLED);
    }
}
