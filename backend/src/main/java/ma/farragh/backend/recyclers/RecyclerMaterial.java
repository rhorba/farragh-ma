package ma.farragh.backend.recyclers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "recycler_materials")
@IdClass(RecyclerMaterialId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecyclerMaterial {

    @Id
    @Column(name = "recycler_id")
    private UUID recyclerId;

    @Id
    @Column(name = "material_type_id")
    private UUID materialTypeId;

    public RecyclerMaterial(UUID recyclerId, UUID materialTypeId) {
        this.recyclerId = recyclerId;
        this.materialTypeId = materialTypeId;
    }
}
