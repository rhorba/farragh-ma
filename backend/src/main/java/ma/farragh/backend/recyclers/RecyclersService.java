package ma.farragh.backend.recyclers;

import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.recyclers.dto.DeclareMaterialsDto;
import ma.farragh.backend.recyclers.dto.DeclareZoneDto;
import ma.farragh.backend.recyclers.dto.MaterialsResponseDto;
import ma.farragh.backend.recyclers.dto.ZoneResponseDto;
import ma.farragh.backend.requests.PickupRequestRepository;
import ma.farragh.backend.requests.RequestsService;
import ma.farragh.backend.requests.dto.RequestResponseDto;
import ma.farragh.backend.shared.exception.BusinessException;
import ma.farragh.backend.shared.materials.MaterialType;
import ma.farragh.backend.shared.materials.MaterialTypeRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RecyclersService {

    private static final int WGS84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private final CoverageZoneRepository coverageZoneRepository;
    private final RecyclerMaterialRepository recyclerMaterialRepository;
    private final MaterialTypeRepository materialTypeRepository;
    private final UserRepository userRepository;
    private final PickupRequestRepository pickupRequestRepository;

    public RecyclersService(CoverageZoneRepository coverageZoneRepository,
                             RecyclerMaterialRepository recyclerMaterialRepository,
                             MaterialTypeRepository materialTypeRepository,
                             UserRepository userRepository,
                             PickupRequestRepository pickupRequestRepository) {
        this.coverageZoneRepository = coverageZoneRepository;
        this.recyclerMaterialRepository = recyclerMaterialRepository;
        this.materialTypeRepository = materialTypeRepository;
        this.userRepository = userRepository;
        this.pickupRequestRepository = pickupRequestRepository;
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

        return pickupRequestRepository.findById(requestId)
                .map(RequestsService::toDto)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Request not found."));
    }

    @Transactional
    public ZoneResponseDto declareZone(UUID recyclerId, DeclareZoneDto dto) {
        User owner = userRepository.findById(recyclerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));

        Polygon area = null;
        Point centerPoint = null;
        Integer radiusM = null;

        if (dto.polygon() != null && !dto.polygon().isEmpty()) {
            area = buildValidatedPolygon(dto.polygon());
        } else if (dto.centerLatitude() != null && dto.centerLongitude() != null && dto.radiusM() != null) {
            validateLatLng(dto.centerLatitude(), dto.centerLongitude());
            centerPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(dto.centerLongitude(), dto.centerLatitude()));
            radiusM = dto.radiusM();
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ZONE_GEOMETRY_REQUIRED",
                    "Provide either a polygon or a center point with radiusM.");
        }

        coverageZoneRepository.deleteByOwnerId(recyclerId);
        coverageZoneRepository.flush();
        CoverageZone zone = new CoverageZone(owner, area, centerPoint, radiusM);
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

    private Polygon buildValidatedPolygon(List<List<Double>> points) {
        if (points.size() < 4) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "A zone polygon needs at least 4 points forming a closed ring.");
        }

        Coordinate[] coords = points.stream()
                .map(p -> {
                    if (p.size() != 2) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                                "Each polygon point must be a [longitude, latitude] pair.");
                    }
                    validateLatLng(p.get(1), p.get(0));
                    return new Coordinate(p.get(0), p.get(1));
                })
                .toArray(Coordinate[]::new);

        if (!coords[0].equals2D(coords[coords.length - 1])) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "Polygon ring must be closed (first and last points equal).");
        }

        Polygon polygon;
        try {
            LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coords);
            polygon = GEOMETRY_FACTORY.createPolygon(ring);
        } catch (RuntimeException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "The zone polygon geometry is invalid.");
        }

        if (!polygon.isValid()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_GEOMETRY",
                    "The zone polygon is invalid or self-intersecting.");
        }
        return polygon;
    }

    private void validateLatLng(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_COORDINATES",
                    "Latitude must be between -90 and 90, longitude between -180 and 180.");
        }
    }

    private static ZoneResponseDto toZoneDto(CoverageZone z) {
        Double lat = z.getCenterPoint() != null ? z.getCenterPoint().getY() : null;
        Double lng = z.getCenterPoint() != null ? z.getCenterPoint().getX() : null;
        List<List<Double>> polygonCoords = z.getArea() != null
                ? java.util.Arrays.stream(z.getArea().getCoordinates())
                        .map(c -> List.of(c.x, c.y))
                        .toList()
                : null;
        return new ZoneResponseDto(z.getId(), lat, lng, z.getRadiusM(), polygonCoords, z.getCreatedAt());
    }
}
