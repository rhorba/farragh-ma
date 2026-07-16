package ma.farragh.backend.municipality;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ma.farragh.backend.auth.User;
import ma.farragh.backend.shared.geo.CoverageZone;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bulk_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BulkSubscription {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipality_id", nullable = false, updatable = false)
    private User municipality;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coverage_zone_id", nullable = false, updatable = false)
    private CoverageZone coverageZone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BulkSubscription(User municipality, CoverageZone coverageZone) {
        this.municipality = municipality;
        this.coverageZone = coverageZone;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
