package ma.farragh.backend.municipality;

import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.RegisterRequest;
import ma.farragh.backend.municipality.dto.SubscribeResultDto;
import ma.farragh.backend.municipality.dto.SubscribeZoneDto;
import ma.farragh.backend.municipality.dto.SubscriptionResponseDto;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MunicipalityControllerTest {

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

    private String registerMunicipalityAndGetToken(String email) {
        RegisterRequest register = new RegisterRequest(email, "a-strong-password", Role.MUNICIPALITY, "Municipality Test", "0600000000", "fr");
        AuthResponse response = restClient.post().uri("/api/v1/auth/register")
                .body(register)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        return response.accessToken();
    }

    private String registerHouseholdAndGetToken(String email) {
        RegisterRequest register = new RegisterRequest(email, "a-strong-password", Role.HOUSEHOLD_SME, "Household Test", "0600000001", "fr");
        AuthResponse response = restClient.post().uri("/api/v1/auth/register")
                .body(register)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        return response.accessToken();
    }

    private SubscribeResultDto subscribe(String token, SubscribeZoneDto dto) {
        return restClient.post().uri("/api/v1/municipality/subscriptions")
                .header("Authorization", "Bearer " + token)
                .body(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SubscribeResultDto.class)
                .returnResult().getResponseBody();
    }

    @Test
    void subscribingARadiusZonePersistsAndIsListedBack() {
        String token = registerMunicipalityAndGetToken("radius-municipality@example.com");
        SubscribeResultDto result = subscribe(token, new SubscribeZoneDto(33.5731, -7.5898, 5000, null, false));

        assertThat(result.overlapWarning()).isFalse();
        assertThat(result.subscription()).isNotNull();
        assertThat(result.subscription().centerLatitude()).isEqualTo(33.5731);
        assertThat(result.subscription().active()).isTrue();

        List<SubscriptionResponseDto> mine = restClient.get().uri("/api/v1/municipality/subscriptions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<SubscriptionResponseDto>>() {})
                .returnResult().getResponseBody();

        assertThat(mine).extracting(SubscriptionResponseDto::id).containsExactly(result.subscription().id());
    }

    @Test
    void subscribingIsAdditiveNotReplacing() {
        String token = registerMunicipalityAndGetToken("additive-municipality@example.com");
        subscribe(token, new SubscribeZoneDto(33.5731, -7.5898, 3000, null, false));
        // Far enough from the first zone to not overlap (Rabat vs Casablanca).
        subscribe(token, new SubscribeZoneDto(34.0209, -6.8416, 3000, null, false));

        List<SubscriptionResponseDto> mine = restClient.get().uri("/api/v1/municipality/subscriptions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<SubscriptionResponseDto>>() {})
                .returnResult().getResponseBody();

        assertThat(mine).hasSize(2);
    }

    @Test
    void overlappingRadiusZoneWarnsAndDoesNotPersistUntilConfirmed() {
        String token = registerMunicipalityAndGetToken("overlap-municipality@example.com");
        subscribe(token, new SubscribeZoneDto(33.5731, -7.5898, 5000, null, false));

        // Same center, would clearly overlap.
        SubscribeResultDto warned = subscribe(token, new SubscribeZoneDto(33.5731, -7.5898, 5000, null, false));
        assertThat(warned.overlapWarning()).isTrue();
        assertThat(warned.subscription()).isNull();

        List<SubscriptionResponseDto> afterWarning = restClient.get().uri("/api/v1/municipality/subscriptions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<SubscriptionResponseDto>>() {})
                .returnResult().getResponseBody();
        assertThat(afterWarning).hasSize(1);

        SubscribeResultDto confirmed = subscribe(token, new SubscribeZoneDto(33.5731, -7.5898, 5000, null, true));
        assertThat(confirmed.subscription()).isNotNull();

        List<SubscriptionResponseDto> afterConfirm = restClient.get().uri("/api/v1/municipality/subscriptions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<SubscriptionResponseDto>>() {})
                .returnResult().getResponseBody();
        assertThat(afterConfirm).hasSize(2);
    }

    @Test
    void polygonZoneOverlappingAnExistingRadiusZoneIsDetected() {
        String token = registerMunicipalityAndGetToken("cross-type-municipality@example.com");
        subscribe(token, new SubscribeZoneDto(33.5731, -7.5898, 5000, null, false));

        List<List<Double>> boxAroundSameCenter = List.of(
                List.of(-7.60, 33.57),
                List.of(-7.58, 33.57),
                List.of(-7.58, 33.58),
                List.of(-7.60, 33.58),
                List.of(-7.60, 33.57)
        );
        SubscribeResultDto warned = subscribe(token, new SubscribeZoneDto(null, null, null, boxAroundSameCenter, false));
        assertThat(warned.overlapWarning()).isTrue();
        assertThat(warned.subscription()).isNull();
    }

    @Test
    void missingGeometryIsRejectedWith400() {
        String token = registerMunicipalityAndGetToken("nogeometry-municipality@example.com");

        restClient.post().uri("/api/v1/municipality/subscriptions")
                .header("Authorization", "Bearer " + token)
                .body(new SubscribeZoneDto(null, null, null, null, false))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void nonMunicipalityRoleIsForbidden() {
        String token = registerHouseholdAndGetToken("household-intruder-muni@example.com");

        restClient.post().uri("/api/v1/municipality/subscriptions")
                .header("Authorization", "Bearer " + token)
                .body(new SubscribeZoneDto(33.5731, -7.5898, 5000, null, false))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }
}
