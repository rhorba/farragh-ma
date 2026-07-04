CREATE TABLE bulk_subscriptions (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  municipality_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  coverage_zone_id  UUID NOT NULL REFERENCES coverage_zones(id) ON DELETE CASCADE,
  is_active         BOOLEAN NOT NULL DEFAULT true,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bulk_subscriptions_municipality ON bulk_subscriptions(municipality_id);
