package ma.farragh.backend.recyclers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecyclerMaterialRepository extends JpaRepository<RecyclerMaterial, RecyclerMaterialId> {
    List<RecyclerMaterial> findByRecyclerId(UUID recyclerId);

    void deleteByRecyclerId(UUID recyclerId);
}
