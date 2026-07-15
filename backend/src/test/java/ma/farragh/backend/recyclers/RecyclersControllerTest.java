package ma.farragh.backend.recyclers;

import ma.farragh.backend.auth.Role;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.auth.dto.AuthResponse;
import ma.farragh.backend.auth.dto.RegisterRequest;
import ma.farragh.backend.recyclers.dto.DeclareMaterialsDto;
import ma.farragh.backend.recyclers.dto.DeclareZoneDto;
import ma.farragh.backend.recyclers.dto.MaterialsResponseDto;
import ma.farragh.backend.recyclers.dto.ZoneResponseDto;
import ma.farragh.backend.requests.dto.CreateRequestDto;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class RecyclersControllerTest {

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
    private ma.farragh.backend.requests.PickupRequestRepository pickupRequestRepository;

    @BeforeEach
    void cleanUp() {
        // pickup_requests.accepted_by_recycler_id has no ON DELETE cascade (by design - deleting
        // a recycler account should not silently wipe accepted-request history), so it must be
        // cleared before users are deleted.
        pickupRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerRecyclerAndGetToken(String email) {
        RegisterRequest register = new RegisterRequest(email, "a-strong-password", Role.RECYCLER, "Recycler Test", "0600000000", "fr");
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

    @Test
    void declareRadiusZonePersistsAndIsReadable() {
        String token = registerRecyclerAndGetToken("radius-recycler@example.com");
        DeclareZoneDto dto = new DeclareZoneDto(33.5731, -7.5898, 5000, null);

        ZoneResponseDto created = restClient.post().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .body(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ZoneResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.centerLatitude()).isEqualTo(33.5731);
        assertThat(created.centerLongitude()).isEqualTo(-7.5898);
        assertThat(created.radiusM()).isEqualTo(5000);

        ZoneResponseDto fetched = restClient.get().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ZoneResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(fetched.id()).isEqualTo(created.id());
    }

    @Test
    void redeclaringZoneReplacesThePreviousOne() {
        String token = registerRecyclerAndGetToken("redeclare-recycler@example.com");
        restClient.post().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .body(new DeclareZoneDto(33.5731, -7.5898, 5000, null))
                .exchange().expectStatus().isOk();

        ZoneResponseDto second = restClient.post().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .body(new DeclareZoneDto(34.0209, -6.8416, 8000, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ZoneResponseDto.class)
                .returnResult().getResponseBody();

        ZoneResponseDto fetched = restClient.get().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ZoneResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(fetched.id()).isEqualTo(second.id());
        assertThat(fetched.radiusM()).isEqualTo(8000);
    }

    @Test
    void selfIntersectingPolygonIsRejectedWith400NotServerError() {
        String token = registerRecyclerAndGetToken("bowtie-recycler@example.com");
        List<List<Double>> bowtie = List.of(
                List.of(0.0, 0.0),
                List.of(1.0, 1.0),
                List.of(1.0, 0.0),
                List.of(0.0, 1.0),
                List.of(0.0, 0.0)
        );
        DeclareZoneDto dto = new DeclareZoneDto(null, null, null, bowtie);

        restClient.post().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .body(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingGeometryIsRejectedWith400() {
        String token = registerRecyclerAndGetToken("nogeometry-recycler@example.com");

        restClient.post().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .body(new DeclareZoneDto(null, null, null, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void householdCannotDeclareZone() {
        String token = registerHouseholdAndGetToken("household-intruder@example.com");

        restClient.post().uri("/api/v1/recyclers/zone")
                .header("Authorization", "Bearer " + token)
                .body(new DeclareZoneDto(33.5731, -7.5898, 5000, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void declareMaterialsPersistsAndReplacesOnRedeclare() {
        String token = registerRecyclerAndGetToken("materials-recycler@example.com");

        MaterialsResponseDto first = restClient.put().uri("/api/v1/recyclers/materials")
                .header("Authorization", "Bearer " + token)
                .body(new DeclareMaterialsDto(List.of("PLASTIC", "PAPER")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MaterialsResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(first.materialTypeCodes()).containsExactlyInAnyOrder("PLASTIC", "PAPER");

        MaterialsResponseDto second = restClient.put().uri("/api/v1/recyclers/materials")
                .header("Authorization", "Bearer " + token)
                .body(new DeclareMaterialsDto(List.of("METAL")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MaterialsResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(second.materialTypeCodes()).containsExactly("METAL");

        MaterialsResponseDto fetched = restClient.get().uri("/api/v1/recyclers/materials")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MaterialsResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(fetched.materialTypeCodes()).containsExactly("METAL");
    }

    @Test
    void unknownMaterialCodeIsRejectedWith400() {
        String token = registerRecyclerAndGetToken("badmaterial-recycler@example.com");

        restClient.put().uri("/api/v1/recyclers/materials")
                .header("Authorization", "Bearer " + token)
                .body(new DeclareMaterialsDto(List.of("UNOBTAINIUM")))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void postRequest(String householdToken, String materialTypeCode, double latitude, double longitude) {
        postRequestAndGetId(householdToken, materialTypeCode, latitude, longitude);
    }

    private UUID postRequestAndGetId(String householdToken, String materialTypeCode, double latitude, double longitude) {
        CreateRequestDto dto = new CreateRequestDto(materialTypeCode, null, "Some address, Casablanca", latitude, longitude, null);
        RequestResponseDto created = restClient.post().uri("/api/v1/requests")
                .header("Authorization", "Bearer " + householdToken)
                .body(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();
        return created.id();
    }

    private List<RequestResponseDto> getFeed(String recyclerToken) {
        return restClient.get().uri("/api/v1/recyclers/feed")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new org.springframework.core.ParameterizedTypeReference<List<RequestResponseDto>>() {})
                .returnResult().getResponseBody();
    }

    @Test
    void feedShowsRequestInsideRadiusZoneWithMatchingMaterial() {
        String recyclerToken = registerRecyclerAndGetToken("feed-radius-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("feed-radius-household@example.com");

        restClient.post().uri("/api/v1/recyclers/zone").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareZoneDto(33.5731, -7.5898, 5000, null)).exchange().expectStatus().isOk();
        restClient.put().uri("/api/v1/recyclers/materials").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareMaterialsDto(List.of("PLASTIC"))).exchange().expectStatus().isOk();

        postRequest(householdToken, "PLASTIC", 33.5735, -7.5890);

        List<RequestResponseDto> feed = getFeed(recyclerToken);
        assertThat(feed).hasSize(1);
        assertThat(feed.get(0).materialTypeCode()).isEqualTo("PLASTIC");
    }

    @Test
    void feedHidesRequestOutsideRadiusZone() {
        String recyclerToken = registerRecyclerAndGetToken("feed-outside-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("feed-outside-household@example.com");

        restClient.post().uri("/api/v1/recyclers/zone").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareZoneDto(33.5731, -7.5898, 5000, null)).exchange().expectStatus().isOk();
        restClient.put().uri("/api/v1/recyclers/materials").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareMaterialsDto(List.of("PLASTIC"))).exchange().expectStatus().isOk();

        // Rabat: ~85km from the Casablanca zone center, well outside a 5km radius.
        postRequest(householdToken, "PLASTIC", 34.0209, -6.8416);

        assertThat(getFeed(recyclerToken)).isEmpty();
    }

    @Test
    void feedHidesRequestWithNonMatchingMaterial() {
        String recyclerToken = registerRecyclerAndGetToken("feed-material-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("feed-material-household@example.com");

        restClient.post().uri("/api/v1/recyclers/zone").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareZoneDto(33.5731, -7.5898, 5000, null)).exchange().expectStatus().isOk();
        restClient.put().uri("/api/v1/recyclers/materials").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareMaterialsDto(List.of("PLASTIC"))).exchange().expectStatus().isOk();

        postRequest(householdToken, "GLASS", 33.5735, -7.5890);

        assertThat(getFeed(recyclerToken)).isEmpty();
    }

    @Test
    void feedShowsRequestInsidePolygonZone() {
        String recyclerToken = registerRecyclerAndGetToken("feed-polygon-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("feed-polygon-household@example.com");

        List<List<Double>> box = List.of(
                List.of(-7.60, 33.57),
                List.of(-7.58, 33.57),
                List.of(-7.58, 33.58),
                List.of(-7.60, 33.58),
                List.of(-7.60, 33.57)
        );
        restClient.post().uri("/api/v1/recyclers/zone").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareZoneDto(null, null, null, box)).exchange().expectStatus().isOk();
        restClient.put().uri("/api/v1/recyclers/materials").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareMaterialsDto(List.of("METAL"))).exchange().expectStatus().isOk();

        postRequest(householdToken, "METAL", 33.575, -7.59);

        assertThat(getFeed(recyclerToken)).hasSize(1);
    }

    private void declareZoneAndMaterial(String recyclerToken, String materialCode) {
        restClient.post().uri("/api/v1/recyclers/zone").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareZoneDto(33.5731, -7.5898, 5000, null)).exchange().expectStatus().isOk();
        restClient.put().uri("/api/v1/recyclers/materials").header("Authorization", "Bearer " + recyclerToken)
                .body(new DeclareMaterialsDto(List.of(materialCode))).exchange().expectStatus().isOk();
    }

    @Test
    void eligibleRecyclerCanAcceptPostedRequest() {
        String recyclerToken = registerRecyclerAndGetToken("accept-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("accept-household@example.com");
        declareZoneAndMaterial(recyclerToken, "PLASTIC");
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 33.5735, -7.5890);

        RequestResponseDto accepted = restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();

        assertThat(accepted.status()).isEqualTo(ma.farragh.backend.requests.RequestStatus.ACCEPTED);
    }

    @Test
    void recyclerCannotAcceptRequestOutsideTheirZone() {
        String recyclerToken = registerRecyclerAndGetToken("outsider-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("outsider-household@example.com");
        declareZoneAndMaterial(recyclerToken, "PLASTIC");
        // Rabat: outside the 5km Casablanca zone declared above.
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 34.0209, -6.8416);

        restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void acceptingAnAlreadyAcceptedRequestReturnsConflict() {
        String recyclerToken = registerRecyclerAndGetToken("double-accept-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("double-accept-household@example.com");
        declareZoneAndMaterial(recyclerToken, "PLASTIC");
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 33.5735, -7.5890);

        restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange().expectStatus().isOk();

        restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void acceptingNonExistentRequestReturnsNotFound() {
        String recyclerToken = registerRecyclerAndGetToken("ghost-recycler@example.com");
        declareZoneAndMaterial(recyclerToken, "PLASTIC");

        restClient.post().uri("/api/v1/recyclers/feed/" + UUID.randomUUID() + "/accept")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void acceptedRequestProgressesThroughScheduledToCompleted() {
        String recyclerToken = registerRecyclerAndGetToken("lifecycle-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("lifecycle-household@example.com");
        declareZoneAndMaterial(recyclerToken, "PLASTIC");
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 33.5735, -7.5890);
        restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + recyclerToken).exchange().expectStatus().isOk();

        RequestResponseDto scheduled = restClient.post().uri("/api/v1/recyclers/requests/" + requestId + "/schedule")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();
        assertThat(scheduled.status()).isEqualTo(ma.farragh.backend.requests.RequestStatus.SCHEDULED);

        RequestResponseDto completed = restClient.post().uri("/api/v1/recyclers/requests/" + requestId + "/complete")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestResponseDto.class)
                .returnResult().getResponseBody();
        assertThat(completed.status()).isEqualTo(ma.farragh.backend.requests.RequestStatus.COMPLETED);

        List<RequestResponseDto> accepted = restClient.get().uri("/api/v1/recyclers/requests")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new org.springframework.core.ParameterizedTypeReference<List<RequestResponseDto>>() {})
                .returnResult().getResponseBody();
        assertThat(accepted).extracting(RequestResponseDto::id).contains(requestId);
    }

    @Test
    void completingAnAcceptedButNotYetScheduledRequestIsRejected() {
        String recyclerToken = registerRecyclerAndGetToken("skip-ahead-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("skip-ahead-household@example.com");
        declareZoneAndMaterial(recyclerToken, "PLASTIC");
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 33.5735, -7.5890);
        restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + recyclerToken).exchange().expectStatus().isOk();

        restClient.post().uri("/api/v1/recyclers/requests/" + requestId + "/complete")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void schedulingAPostedRequestThatWasNeverAcceptedIsRejected() {
        String recyclerToken = registerRecyclerAndGetToken("never-accepted-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("never-accepted-household@example.com");
        declareZoneAndMaterial(recyclerToken, "PLASTIC");
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 33.5735, -7.5890);

        restClient.post().uri("/api/v1/recyclers/requests/" + requestId + "/schedule")
                .header("Authorization", "Bearer " + recyclerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aDifferentRecyclerCannotScheduleSomeoneElsesAcceptedRequest() {
        String ownerToken = registerRecyclerAndGetToken("owner-recycler@example.com");
        String intruderToken = registerRecyclerAndGetToken("intruder-recycler@example.com");
        String householdToken = registerHouseholdAndGetToken("intrusion-household@example.com");
        declareZoneAndMaterial(ownerToken, "PLASTIC");
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 33.5735, -7.5890);
        restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                .header("Authorization", "Bearer " + ownerToken).exchange().expectStatus().isOk();

        restClient.post().uri("/api/v1/recyclers/requests/" + requestId + "/schedule")
                .header("Authorization", "Bearer " + intruderToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void twoRecyclersAcceptingSimultaneouslyOnlyOneWins() throws InterruptedException {
        String recyclerAToken = registerRecyclerAndGetToken("race-recyclerA@example.com");
        String recyclerBToken = registerRecyclerAndGetToken("race-recyclerB@example.com");
        String householdToken = registerHouseholdAndGetToken("race-household@example.com");
        declareZoneAndMaterial(recyclerAToken, "PLASTIC");
        declareZoneAndMaterial(recyclerBToken, "PLASTIC");
        UUID requestId = postRequestAndGetId(householdToken, "PLASTIC", 33.5735, -7.5890);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicReference<HttpStatus> statusA = new AtomicReference<>();
        AtomicReference<HttpStatus> statusB = new AtomicReference<>();

        Runnable acceptAsA = () -> {
            ready.countDown();
            awaitUninterruptibly(go);
            HttpStatusCode status = restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                    .header("Authorization", "Bearer " + recyclerAToken)
                    .exchange().returnResult(Void.class).getStatus();
            statusA.set(HttpStatus.valueOf(status.value()));
        };
        Runnable acceptAsB = () -> {
            ready.countDown();
            awaitUninterruptibly(go);
            HttpStatusCode status = restClient.post().uri("/api/v1/recyclers/feed/" + requestId + "/accept")
                    .header("Authorization", "Bearer " + recyclerBToken)
                    .exchange().returnResult(Void.class).getStatus();
            statusB.set(HttpStatus.valueOf(status.value()));
        };

        executor.submit(acceptAsA);
        executor.submit(acceptAsB);
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        List<HttpStatus> results = List.of(statusA.get(), statusB.get());
        assertThat(results).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
