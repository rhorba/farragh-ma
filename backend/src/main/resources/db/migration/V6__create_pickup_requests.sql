CREATE TABLE pickup_requests (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  requester_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  material_type_id        UUID NOT NULL REFERENCES material_types(id),
  quantity_desc           VARCHAR(255),
  address_text            VARCHAR(500) NOT NULL,
  location                GEOGRAPHY(POINT, 4326) NOT NULL,
  status                  VARCHAR(20) NOT NULL DEFAULT 'POSTED'
                            CHECK (status IN ('POSTED','ACCEPTED','SCHEDULED','COMPLETED','CANCELLED')),
  accepted_by_recycler_id UUID REFERENCES users(id),
  photo_url               VARCHAR(500),
  created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_requests_requester ON pickup_requests(requester_id);
CREATE INDEX idx_requests_status ON pickup_requests(status);
CREATE INDEX idx_requests_location ON pickup_requests USING GIST(location);
CREATE INDEX idx_requests_recycler ON pickup_requests(accepted_by_recycler_id);
