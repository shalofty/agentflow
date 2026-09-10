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
