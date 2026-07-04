CREATE TABLE users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           VARCHAR(255) NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  role            VARCHAR(20) NOT NULL CHECK (role IN ('HOUSEHOLD_SME','RECYCLER','MUNICIPALITY','ADMIN')),
  full_name       VARCHAR(255) NOT NULL,
  phone           VARCHAR(30),
  preferred_lang  VARCHAR(2) NOT NULL DEFAULT 'fr' CHECK (preferred_lang IN ('fr','ar')),
  is_active       BOOLEAN NOT NULL DEFAULT true,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- email lookup already covered by the UNIQUE constraint's implicit index
CREATE INDEX idx_users_role ON users(role);
