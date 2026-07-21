package ma.farragh.backend.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_action_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActionLog {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false, updatable = false)
    private User admin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false, updatable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminActionType action;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AdminActionLog(User admin, User target, AdminActionType action) {
        this.admin = admin;
        this.target = target;
        this.action = action;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
