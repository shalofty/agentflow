CREATE TABLE workflow_definition (
  id UUID PRIMARY KEY,
  workflow_key VARCHAR(100) NOT NULL,
  title VARCHAR(200) NOT NULL,
  version INT NOT NULL,
  definition_json JSONB NOT NULL,
  active BOOLEAN NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_workflow_key_version UNIQUE (workflow_key, version)
);

CREATE UNIQUE INDEX uq_workflow_one_active
  ON workflow_definition (workflow_key) WHERE active = true;
