package ma.farragh.backend.recyclers;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RecyclerMaterialId implements Serializable {

    private UUID recyclerId;
    private UUID materialTypeId;

    public RecyclerMaterialId() {
    }

    public RecyclerMaterialId(UUID recyclerId, UUID materialTypeId) {
        this.recyclerId = recyclerId;
        this.materialTypeId = materialTypeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecyclerMaterialId that)) return false;
        return Objects.equals(recyclerId, that.recyclerId) && Objects.equals(materialTypeId, that.materialTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recyclerId, materialTypeId);
    }
}
