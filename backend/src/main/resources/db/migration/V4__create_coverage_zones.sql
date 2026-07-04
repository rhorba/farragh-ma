CREATE TABLE coverage_zones (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  area         GEOGRAPHY(POLYGON, 4326),
  center_point GEOGRAPHY(POINT, 4326),
  radius_m     INTEGER,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (area IS NOT NULL OR (center_point IS NOT NULL AND radius_m IS NOT NULL))
);

CREATE INDEX idx_zones_owner ON coverage_zones(owner_id);
CREATE INDEX idx_zones_area ON coverage_zones USING GIST(area);
CREATE INDEX idx_zones_center ON coverage_zones USING GIST(center_point);
