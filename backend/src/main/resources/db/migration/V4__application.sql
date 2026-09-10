CREATE TABLE application (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL REFERENCES customer(id),
  workflow_definition_id UUID NOT NULL REFERENCES workflow_definition(id),
  status VARCHAR(40) NOT NULL,
  correlation_id VARCHAR(64),
  submitted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE application_data (
  application_id UUID PRIMARY KEY REFERENCES application(id),
  payload JSONB NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
