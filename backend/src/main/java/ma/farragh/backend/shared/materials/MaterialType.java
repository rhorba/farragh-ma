package ma.farragh.backend.shared.materials;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "material_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaterialType {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "label_fr", nullable = false)
    private String labelFr;

    @Column(name = "label_ar", nullable = false)
    private String labelAr;
}
