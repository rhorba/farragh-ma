package ma.farragh.backend.requests;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupRequestRepository extends JpaRepository<PickupRequest, UUID> {
    List<PickupRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
    Optional<PickupRequest> findByIdAndRequesterId(UUID id, UUID requesterId);
}
