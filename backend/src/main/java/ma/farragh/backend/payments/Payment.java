package ma.farragh.backend.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Deliberately holds pickupRequestId as a plain column, not a JPA relation to PickupRequest -
 * payments/ and requests/ are sibling modules (Architecture doc §3) that talk through public
 * service interfaces only, never through shared entity references.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "pickup_request_id", nullable = false, unique = true)
    private UUID pickupRequestId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Payment(UUID pickupRequestId, int amountCents, String currency, String provider,
                    PaymentMode mode, PaymentStatus status, String providerRef) {
        this.pickupRequestId = pickupRequestId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.provider = provider;
        this.mode = mode;
        this.status = status;
        this.providerRef = providerRef;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
