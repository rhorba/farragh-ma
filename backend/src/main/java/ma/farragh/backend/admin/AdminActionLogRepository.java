package ma.farragh.backend.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, UUID> {

    Page<AdminActionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
