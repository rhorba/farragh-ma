CREATE TABLE admin_action_log (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  action           VARCHAR(20) NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_action_log_target ON admin_action_log(target_user_id);
