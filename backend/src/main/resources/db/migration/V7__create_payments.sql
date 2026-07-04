CREATE TABLE payments (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pickup_request_id UUID NOT NULL UNIQUE REFERENCES pickup_requests(id) ON DELETE CASCADE,
  amount_cents      INTEGER NOT NULL,
  currency          VARCHAR(3) NOT NULL DEFAULT 'MAD',
  provider          VARCHAR(20) NOT NULL DEFAULT 'CMI',
  mode              VARCHAR(10) NOT NULL DEFAULT 'MOCK' CHECK (mode IN ('MOCK','LIVE')),
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCEEDED','FAILED')),
  provider_ref      VARCHAR(255),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
