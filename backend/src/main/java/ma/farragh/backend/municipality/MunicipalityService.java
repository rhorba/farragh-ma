package ma.farragh.backend.municipality;

import ma.farragh.backend.auth.User;
import ma.farragh.backend.auth.UserRepository;
import ma.farragh.backend.municipality.dto.SubscribeResultDto;
import ma.farragh.backend.municipality.dto.SubscribeZoneDto;
import ma.farragh.backend.municipality.dto.SubscriptionResponseDto;
import ma.farragh.backend.shared.exception.BusinessException;
import ma.farragh.backend.shared.geo.CoverageZone;
import ma.farragh.backend.shared.geo.CoverageZoneRepository;
import ma.farragh.backend.shared.geo.ZoneGeometryValidator;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MunicipalityService {

    private final BulkSubscriptionRepository bulkSubscriptionRepository;
    private final CoverageZoneRepository coverageZoneRepository;
    private final UserRepository userRepository;
    private final ZoneGeometryValidator zoneGeometryValidator;

    public MunicipalityService(BulkSubscriptionRepository bulkSubscriptionRepository,
                                CoverageZoneRepository coverageZoneRepository,
                                UserRepository userRepository,
                                ZoneGeometryValidator zoneGeometryValidator) {
        this.bulkSubscriptionRepository = bulkSubscriptionRepository;
        this.coverageZoneRepository = coverageZoneRepository;
        this.userRepository = userRepository;
        this.zoneGeometryValidator = zoneGeometryValidator;
    }

    @Transactional
    public SubscribeResultDto subscribe(UUID municipalityId, SubscribeZoneDto dto) {
        User municipality = userRepository.findById(municipalityId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found."));

        ZoneGeometryValidator.ZoneGeometry geometry = zoneGeometryValidator.resolve(
                dto.centerLatitude(), dto.centerLongitude(), dto.radiusM(), dto.polygon());

        String candidatePolygonWkt = geometry.area() != null ? new WKTWriter().write(geometry.area()) : null;
        Double candidateLat = geometry.centerPoint() != null ? geometry.centerPoint().getY() : null;
        Double candidateLng = geometry.centerPoint() != null ? geometry.centerPoint().getX() : null;

        boolean overlaps = bulkSubscriptionRepository.existsOverlapping(
                candidatePolygonWkt, candidateLat, candidateLng, geometry.radiusM());

        if (overlaps && !dto.confirmOverlap()) {
            return new SubscribeResultDto(true, null);
        }

        CoverageZone zone = new CoverageZone(municipality, geometry.area(), geometry.centerPoint(), geometry.radiusM());
        coverageZoneRepository.save(zone);
        BulkSubscription subscription = new BulkSubscription(municipality, zone);
        bulkSubscriptionRepository.save(subscription);

        return new SubscribeResultDto(overlaps, toDto(subscription));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> listMySubscriptions(UUID municipalityId) {
        return bulkSubscriptionRepository.findByMunicipalityIdOrderByCreatedAtDesc(municipalityId).stream()
                .map(MunicipalityService::toDto)
                .toList();
    }

    private static SubscriptionResponseDto toDto(BulkSubscription s) {
        CoverageZone z = s.getCoverageZone();
        ZoneGeometryValidator.ZoneCoordinates coords = ZoneGeometryValidator.toCoordinates(z.getCenterPoint(), z.getArea());
        return new SubscriptionResponseDto(s.getId(), coords.centerLatitude(), coords.centerLongitude(),
                z.getRadiusM(), coords.polygon(), s.isActive(), s.getCreatedAt());
    }
}
