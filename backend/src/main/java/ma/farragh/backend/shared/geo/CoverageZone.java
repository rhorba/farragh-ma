package ma.farragh.backend.shared.geo;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ma.farragh.backend.auth.User;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.UUID;

/**
 * A geographic coverage zone owned by any user - a recycler declaring where it collects,
 * or a municipality bulk-subscribing a neighborhood. Owner-agnostic by design, so it lives in
 * shared rather than either feature package (architecture doc: no direct cross-module
 * repository access between recyclers and municipality).
 */
@Entity
@Table(name = "coverage_zones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoverageZone {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @Column(columnDefinition = "geography(Polygon,4326)")
    private Polygon area;

    @Column(name = "center_point", columnDefinition = "geography(Point,4326)")
    private Point centerPoint;

    @Column(name = "radius_m")
    private Integer radiusM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CoverageZone(User owner, Polygon area, Point centerPoint, Integer radiusM) {
        this.owner = owner;
        this.area = area;
        this.centerPoint = centerPoint;
        this.radiusM = radiusM;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
