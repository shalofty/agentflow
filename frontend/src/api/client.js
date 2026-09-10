async function parseError(response) {
  const body = await response.json().catch(() => ({}))
  const message =
    body.detail ||
    body.title ||
    response.statusText ||
    'Request failed'
  const error = new Error(message)
  error.status = response.status
  error.problem = body
  return error
}

export async function listCustomers() {
  const response = await fetch('/api/customers')
  if (!response.ok) {
    throw await parseError(response)
  }
  return response.json()
}

export async function createCustomer(customer) {
  const response = await fetch('/api/customers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(customer),
  })
  if (!response.ok) {
    throw await parseError(response)
  }
  return response.json()
}

export async function createApplication({ customerId, workflowKey }) {
  const response = await fetch('/api/applications', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ customerId, workflowKey }),
  })
  if (!response.ok) {
    throw await parseError(response)
  }
  return response.json()
}

export async function getApplicationDefinition(applicationId) {
  const response = await fetch(`/api/applications/${applicationId}/definition`)
  if (!response.ok) {
    throw await parseError(response)
  }
  return response.json()
}

export async function saveApplicationData(applicationId, payload) {
  const response = await fetch(`/api/applications/${applicationId}/data`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ payload }),
  })
  if (!response.ok) {
    throw await parseError(response)
  }
  return response.json()
}
