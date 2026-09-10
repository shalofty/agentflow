async function parseError(response) {
  const contentType = response.headers.get('content-type') || ''
  let body = {}
  if (contentType.includes('application/json') || contentType.includes('problem+json')) {
    body = await response.json().catch(() => ({}))
  } else {
    const text = await response.text().catch(() => '')
    if (text) {
      body = { detail: text.slice(0, 200) }
    }
  }
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

async function request(path, options = {}) {
  const response = await fetch(path, options)
  if (!response.ok) {
    throw await parseError(response)
  }
  if (response.status === 204) {
    return null
  }
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    return null
  }
  return response.json()
}

export async function getHealth({ signal } = {}) {
  const response = await fetch('/api/health', { signal })
  if (!response.ok) {
    throw await parseError(response)
  }
  return response.json()
}

export async function listCustomers() {
  return request('/api/customers')
}

export async function createCustomer(customer) {
  return request('/api/customers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(customer),
  })
}

export async function listApplications(customerId) {
  const params = new URLSearchParams({ customerId })
  return request(`/api/applications?${params}`)
}

export async function createApplication({ customerId, workflowKey }) {
  return request('/api/applications', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ customerId, workflowKey }),
  })
}

export async function getApplicationDefinition(applicationId) {
  return request(`/api/applications/${applicationId}/definition`)
}

export async function getApplicationData(applicationId) {
  return request(`/api/applications/${applicationId}/data`)
}

export async function saveApplicationData(applicationId, payload) {
  return request(`/api/applications/${applicationId}/data`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ payload }),
  })
}

export async function submitApplication(applicationId, demoScenario) {
  return request(`/api/applications/${applicationId}/submit`, {
    method: 'POST',
    headers: demoScenario ? { 'X-Demo-Scenario': demoScenario } : {},
  })
}

export async function getDemoConfiguration() {
  const response = await fetch('/api/demo/enabled')
  if (response.status === 404) {
    return { enabled: false, scenarios: [] }
  }
  if (!response.ok) {
    throw await parseError(response)
  }
  return response.json()
}

export async function getApplication(applicationId) {
  return request(`/api/applications/${applicationId}`)
}

export async function getActivities(applicationId) {
  return request(`/api/applications/${applicationId}/activities`)
}

export function formatApiError(error) {
  const errors = error.problem?.errors
  const labels = error.problem?.errorLabels || {}
  if (errors && typeof errors === 'object') {
    const fields = Object.entries(errors)
      .map(([field, message]) => `${labels[field] || field}: ${message}`)
      .join('; ')
    return `${error.message} (${fields})`
  }
  return error.message
}

export function fieldErrorsFromProblem(problem) {
  if (!problem?.errors) {
    return {}
  }
  return { ...problem.errors }
}
