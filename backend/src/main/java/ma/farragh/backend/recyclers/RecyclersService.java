package ma.farragh.backend.recyclers;

import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.notifications.NotificationService;
import ma.farragh.backend.recyclers.dto.DeclareMaterialsDto;
import ma.farragh.backend.recyclers.dto.DeclareZoneDto;
import ma.farragh.backend.recyclers.dto.MaterialsResponseDto;
import ma.farragh.backend.recyclers.dto.ZoneResponseDto;
import ma.farragh.backend.requests.PickupRequest;
import ma.farragh.backend.requests.PickupRequestRepository;
import ma.farragh.backend.requests.RequestsService;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.exception.BusinessException;
import ma.farragh.backend.shared.geo.CoverageZone;
import ma.farragh.backend.shared.geo.CoverageZoneRepository;
import ma.farragh.backend.shared.geo.ZoneGeometryValidator;
import ma.farragh.backend.shared.materials.MaterialType;
import ma.farragh.backend.shared.materials.MaterialTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RecyclersService {

    private final CoverageZoneRepository coverageZoneRepository;
    private final RecyclerMaterialRepository recyclerMaterialRepository;
    private final MaterialTypeRepository materialTypeRepository;
    private final UserRepository userRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final NotificationService notificationService;
    private final ZoneGeometryValidator zoneGeometryValidator;

    public RecyclersService(CoverageZoneRepository coverageZoneRepository,
                             RecyclerMaterialRepository recyclerMaterialRepository,
                             MaterialTypeRepository materialTypeRepository,
                             UserRepository userRepository,
                             PickupRequestRepository pickupRequestRepository,
                             NotificationService notificationService,
                             ZoneGeometryValidator zoneGeometryValidator) {
        this.coverageZoneRepository = coverageZoneRepository;
        this.recyclerMaterialRepository = recyclerMaterialRepository;
        this.materialTypeRepository = materialTypeRepository;
        this.userRepository = userRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.notificationService = notificationService;
        this.zoneGeometryValidator = zoneGeometryValidator;
    }

    @Transactional(readOnly = true)
    public List<RequestResponseDto> getMatchedFeed(UUID recyclerId) {
        return pickupRequestRepository.findMatchedFeed(recyclerId).stream()
                .map(RequestsService::toDto)
                .toList();
    }

    @Transactional
    public RequestResponseDto accept(UUID recyclerId, UUID requestId) {
        if (!pickupRequestRepository.isEligibleForRecycler(requestId, recyclerId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Request not found.");
        }

        int updated = pickupRequestRepository.acceptIfPosted(requestId, recyclerId, Instant.now());
        if (updated == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "REQUEST_ALREADY_TAKEN",
                    "This request is no longer available.");
        }

        PickupRequest request = findEntityOrThrow(requestId);
        notificationService.notifyRequestAccepted(request.getRequester().getEmail(), requestId);
        return RequestsService.toDto(request);
    }

    @Transactional(readOnly = true)
    public List<RequestResponseDto> listAccepted(UUID recyclerId) {
        return pickupRequestRepository.findByAcceptedByRecyclerIdOrderByUpdatedAtDesc(recyclerId).stream()
                .map(RequestsService::toDto)
                .toList();
    }

    @Transactional
    public RequestResponseDto schedule(UUID recyclerId, UUID requestId) {
        ownedByRecyclerOrThrow(recyclerId, requestId);
        int updated = pickupRequestRepository.scheduleIfAccepted(requestId, recyclerId, Instant.now());
        if (updated == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "Only an ACCEPTED request can be scheduled.");
        }
        return RequestsService.toDto(findEntityOrThrow(requestId));
    }

    @Transactional
    public RequestResponseDto complete(UUID recyclerId, UUID requestId) {
        ownedByRecyclerOrThrow(recyclerId, requestId);
        int updated = pickupRequestRepository.completeIfScheduled(requestId, recyclerId, Instant.now());
        if (updated == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "Only a SCHEDULED request can be completed.");
        }
        PickupRequest request = findEntityOrThrow(requestId);
        notificationService.notifyRequestCompleted(request.getRequester().getEmail(), requestId);
        return RequestsService.toDto(request);
    }

    private void ownedByRecyclerOrThrow(UUID recyclerId, UUID requestId) {
        boolean owned = pickupRequestRepository.findById(requestId)
                .map(r -> r.getAcceptedByRecycler() != null && r.getAcceptedByRecycler().getId().equals(recyclerId))
                .orElse(false);
        if (!owned) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Request not found.");
        }
    }

    private PickupRequest findEntityOrThrow(UUID requestId) {
        return pickupRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Request not found."));
    }

    @Transactional
    public ZoneResponseDto declareZone(UUID recyclerId, DeclareZoneDto dto) {
        User owner = userRepository.findById(recyclerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));

        ZoneGeometryValidator.ZoneGeometry geometry = zoneGeometryValidator.resolve(
                dto.centerLatitude(), dto.centerLongitude(), dto.radiusM(), dto.polygon());

        coverageZoneRepository.deleteByOwnerId(recyclerId);
        coverageZoneRepository.flush();
        CoverageZone zone = new CoverageZone(owner, geometry.area(), geometry.centerPoint(), geometry.radiusM());
        coverageZoneRepository.save(zone);
        return toZoneDto(zone);
    }

    @Transactional(readOnly = true)
    public ZoneResponseDto getMyZone(UUID recyclerId) {
        List<CoverageZone> zones = coverageZoneRepository.findByOwnerId(recyclerId);
        if (zones.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "No coverage zone declared yet.");
        }
        return toZoneDto(zones.get(0));
    }

    @Transactional
    public MaterialsResponseDto declareMaterials(UUID recyclerId, DeclareMaterialsDto dto) {
        List<MaterialType> types = dto.materialTypeCodes().stream()
                .map(code -> materialTypeRepository.findByCode(code)
                        .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "UNKNOWN_MATERIAL_TYPE",
                                "Unknown material type: " + code)))
                .toList();

        recyclerMaterialRepository.deleteByRecyclerId(recyclerId);
        recyclerMaterialRepository.flush();
        List<RecyclerMaterial> links = types.stream()
                .map(t -> new RecyclerMaterial(recyclerId, t.getId()))
                .toList();
        recyclerMaterialRepository.saveAll(links);
        return new MaterialsResponseDto(types.stream().map(MaterialType::getCode).toList());
    }

    @Transactional(readOnly = true)
    public MaterialsResponseDto getMyMaterials(UUID recyclerId) {
        List<UUID> materialIds = recyclerMaterialRepository.findByRecyclerId(recyclerId).stream()
                .map(RecyclerMaterial::getMaterialTypeId)
                .toList();
        List<String> codes = materialTypeRepository.findAllById(materialIds).stream()
                .map(MaterialType::getCode)
                .toList();
        return new MaterialsResponseDto(codes);
    }

    private static ZoneResponseDto toZoneDto(CoverageZone z) {
        ZoneGeometryValidator.ZoneCoordinates coords = ZoneGeometryValidator.toCoordinates(z.getCenterPoint(), z.getArea());
        return new ZoneResponseDto(z.getId(), coords.centerLatitude(), coords.centerLongitude(), z.getRadiusM(),
                coords.polygon(), z.getCreatedAt());
    }
}
