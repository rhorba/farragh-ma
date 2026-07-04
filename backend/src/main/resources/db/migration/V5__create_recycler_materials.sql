CREATE TABLE recycler_materials (
  recycler_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  material_type_id UUID NOT NULL REFERENCES material_types(id) ON DELETE CASCADE,
  PRIMARY KEY (recycler_id, material_type_id)
);

CREATE INDEX idx_recycler_materials_material ON recycler_materials(material_type_id);
