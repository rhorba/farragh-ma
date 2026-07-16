package ma.farragh.backend.shared.geo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoverageZoneRepository extends JpaRepository<CoverageZone, UUID> {
    List<CoverageZone> findByOwnerId(UUID ownerId);

    void deleteByOwnerId(UUID ownerId);
}
