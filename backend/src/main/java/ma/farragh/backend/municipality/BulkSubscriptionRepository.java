package ma.farragh.backend.municipality;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BulkSubscriptionRepository extends JpaRepository<BulkSubscription, UUID> {

    List<BulkSubscription> findByMunicipalityIdOrderByCreatedAtDesc(UUID municipalityId);

    /**
     * Overlap check for the UX-mandated warning (Story 6.1): candidate geometry is always
     * exactly one of polygon OR center+radius (same "provide either" rule as zone declaration),
     * so exactly one candidate branch pair below is ever non-null for a given call. Mirrors the
     * ST_Contains/ST_DWithin style already used by the recyclers matched-feed query (ADR-4),
     * generalized to both sides since here either the candidate or the existing zone (or both)
     * can be a polygon or a circle.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM bulk_subscriptions bs
                JOIN coverage_zones cz ON cz.id = bs.coverage_zone_id
                WHERE bs.is_active = true
                AND (
                    (:candPolygonWkt IS NOT NULL AND cz.area IS NOT NULL
                        AND ST_Intersects(cz.area::geometry, ST_GeomFromText(:candPolygonWkt, 4326)))
                    OR (:candPolygonWkt IS NOT NULL AND cz.center_point IS NOT NULL AND cz.radius_m IS NOT NULL
                        AND ST_DWithin(cz.center_point, ST_GeomFromText(:candPolygonWkt, 4326)::geography, cz.radius_m))
                    OR (:candLng IS NOT NULL AND :candLat IS NOT NULL AND :candRadiusM IS NOT NULL AND cz.area IS NOT NULL
                        AND ST_DWithin(cz.area, ST_SetSRID(ST_MakePoint(:candLng, :candLat), 4326)::geography, :candRadiusM))
                    OR (:candLng IS NOT NULL AND :candLat IS NOT NULL AND :candRadiusM IS NOT NULL
                        AND cz.center_point IS NOT NULL AND cz.radius_m IS NOT NULL
                        AND ST_DWithin(cz.center_point, ST_SetSRID(ST_MakePoint(:candLng, :candLat), 4326)::geography,
                                        cz.radius_m + :candRadiusM))
                )
            )
            """, nativeQuery = true)
    boolean existsOverlapping(@Param("candPolygonWkt") String candidatePolygonWkt,
                               @Param("candLat") Double candidateLatitude,
                               @Param("candLng") Double candidateLongitude,
                               @Param("candRadiusM") Integer candidateRadiusM);
}
