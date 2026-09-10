CREATE TABLE integration_activity (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES application(id),
  correlation_id VARCHAR(64) NOT NULL,
  integration_name VARCHAR(80) NOT NULL,
  integration_type VARCHAR(20) NOT NULL,
  action VARCHAR(160) NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
  http_status INTEGER,
  duration_ms BIGINT NOT NULL,
  attempt INTEGER NOT NULL CHECK (attempt >= 1),
  error_message VARCHAR(500),
  request_summary VARCHAR(500),
  response_summary VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_integration_activity_application
  ON integration_activity(application_id, created_at);

CREATE INDEX idx_integration_activity_correlation
  ON integration_activity(correlation_id);
