package ma.farragh.backend.requests;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<PickupRequest> findByAcceptedByRecyclerIdOrderByUpdatedAtDesc(UUID recyclerId);

    /**
     * Story 4.1 state machine: same conditional-atomic-UPDATE pattern as acceptIfPosted -
     * the WHERE clause re-checks both the expected current status AND recycler ownership,
     * so a lost race or a request that was never this recycler's simply updates 0 rows.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE pickup_requests
            SET status = 'SCHEDULED', updated_at = :now
            WHERE id = :requestId AND status = 'ACCEPTED' AND accepted_by_recycler_id = :recyclerId
            """, nativeQuery = true)
    int scheduleIfAccepted(@Param("requestId") UUID requestId, @Param("recyclerId") UUID recyclerId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE pickup_requests
            SET status = 'COMPLETED', updated_at = :now
            WHERE id = :requestId AND status = 'SCHEDULED' AND accepted_by_recycler_id = :recyclerId
            """, nativeQuery = true)
    int completeIfScheduled(@Param("requestId") UUID requestId, @Param("recyclerId") UUID recyclerId, @Param("now") Instant now);

    @Query("""
            SELECT r FROM PickupRequest r
            WHERE (:status IS NULL OR r.status = :status)
            AND (CAST(:createdFrom AS timestamp) IS NULL OR r.createdAt >= CAST(:createdFrom AS timestamp))
            AND (CAST(:createdTo AS timestamp) IS NULL OR r.createdAt < CAST(:createdTo AS timestamp))
            ORDER BY r.createdAt DESC
            """)
    Page<PickupRequest> search(@Param("status") RequestStatus status,
                                @Param("createdFrom") Instant createdFrom,
                                @Param("createdTo") Instant createdTo,
                                Pageable pageable);

    /**
     * Admin analytics (Story: analytics dashboard) - status breakdown over a date range.
     * Status returned as the raw text column rather than mapped to RequestStatus here: native-query
     * interface projections go through Spring's ConversionService for scalar types, which is more
     * fragile to rely on for an enum than just calling RequestStatus.valueOf() in the service.
     */
    @Query(value = """
            SELECT pr.status AS status, COUNT(*) AS cnt
            FROM pickup_requests pr
            WHERE pr.created_at >= :from AND pr.created_at < :to
            GROUP BY pr.status
            """, nativeQuery = true)
    List<StatusCountRow> countByStatusInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT date_trunc(:unit, pr.created_at) AS bucket, COUNT(*) AS cnt
            FROM pickup_requests pr
            WHERE pr.created_at >= :from AND pr.created_at < :to
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<BucketCountRow> countCreatedByBucket(@Param("from") Instant from, @Param("to") Instant to, @Param("unit") String unit);

    /**
     * "Completed" bucket is approximated via updated_at where status=COMPLETED - there's no
     * dedicated completed_at column, and completed requests are a terminal state that's never
     * touched again, so updated_at is an accurate proxy for completion time.
     */
    @Query(value = """
            SELECT date_trunc(:unit, pr.updated_at) AS bucket, COUNT(*) AS cnt
            FROM pickup_requests pr
            WHERE pr.status = 'COMPLETED' AND pr.updated_at >= :from AND pr.updated_at < :to
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<BucketCountRow> countCompletedByBucket(@Param("from") Instant from, @Param("to") Instant to, @Param("unit") String unit);

    interface StatusCountRow {
        String getStatus();
        Long getCnt();
    }

    interface BucketCountRow {
        Instant getBucket();
        Long getCnt();
    }
}
