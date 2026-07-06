package ma.farragh.backend.requests;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupRequestRepository extends JpaRepository<PickupRequest, UUID> {
    List<PickupRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
    Optional<PickupRequest> findByIdAndRequesterId(UUID id, UUID requesterId);

    /**
     * ADR-4: zone matching done via ST_Contains/ST_DWithin in the repository query
     * (not by pulling all POSTED rows into app memory).
     */
    @Query(value = """
            SELECT DISTINCT pr.* FROM pickup_requests pr
            JOIN recycler_materials rm ON rm.material_type_id = pr.material_type_id AND rm.recycler_id = :recyclerId
            JOIN coverage_zones cz ON cz.owner_id = :recyclerId
            WHERE pr.status = 'POSTED'
            AND (
                (cz.area IS NOT NULL AND ST_Contains(cz.area::geometry, pr.location::geometry))
                OR (cz.center_point IS NOT NULL AND cz.radius_m IS NOT NULL
                    AND ST_DWithin(cz.center_point, pr.location, cz.radius_m))
            )
            ORDER BY pr.created_at DESC
            """, nativeQuery = true)
    List<PickupRequest> findMatchedFeed(@Param("recyclerId") UUID recyclerId);

    /**
     * Server-side re-check for accept (IDOR guard): a recycler must not be able to accept
     * a request outside their zone/materials by calling the API directly, even though the
     * feed already filters it out client-side.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM pickup_requests pr
                JOIN recycler_materials rm ON rm.material_type_id = pr.material_type_id AND rm.recycler_id = :recyclerId
                JOIN coverage_zones cz ON cz.owner_id = :recyclerId
                WHERE pr.id = :requestId
                AND (
                    (cz.area IS NOT NULL AND ST_Contains(cz.area::geometry, pr.location::geometry))
                    OR (cz.center_point IS NOT NULL AND cz.radius_m IS NOT NULL
                        AND ST_DWithin(cz.center_point, pr.location, cz.radius_m))
                )
            )
            """, nativeQuery = true)
    boolean isEligibleForRecycler(@Param("requestId") UUID requestId, @Param("recyclerId") UUID recyclerId);

    /**
     * Race protection (Story 3.3): a single conditional atomic UPDATE guarded by
     * "WHERE status = 'POSTED'". Postgres row-locking serializes concurrent attempts on the
     * same row; only the first commits with status still POSTED, so the loser's WHERE clause
     * no longer matches and 0 rows are affected - the DB itself is the source of truth for
     * who won, no separate version column needed.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE pickup_requests
            SET status = 'ACCEPTED', accepted_by_recycler_id = :recyclerId, updated_at = :now
            WHERE id = :requestId AND status = 'POSTED'
            """, nativeQuery = true)
    int acceptIfPosted(@Param("requestId") UUID requestId, @Param("recyclerId") UUID recyclerId, @Param("now") Instant now);
}
