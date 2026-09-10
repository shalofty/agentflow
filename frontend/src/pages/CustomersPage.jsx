import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  createApplication,
  createCustomer,
  formatApiError,
  listApplications,
  listCustomers,
} from '../api/client'

const emptyForm = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
}

const WORKFLOW_OPTIONS = [
  { key: 'auto-policy', label: 'Auto Policy' },
  { key: 'home-policy', label: 'Home Policy' },
]

function sampleCustomer() {
  const stamp = Date.now().toString(36)
  return {
    firstName: 'Demo',
    lastName: 'Agent',
    email: `demo.agent.${stamp}@example.com`,
    phone: '2025550100',
  }
}

function workflowLabel(key) {
  return WORKFLOW_OPTIONS.find((option) => option.key === key)?.label || key
}

export default function CustomersPage() {
  const navigate = useNavigate()
  const [customers, setCustomers] = useState([])
  const [applicationsByCustomer, setApplicationsByCustomer] = useState({})
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [creatingFor, setCreatingFor] = useState(null)
  const [error, setError] = useState(null)

  const loadCustomers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listCustomers()
      setCustomers(data)
      const entries = await Promise.all(
        data.map(async (customer) => {
          try {
            const apps = await listApplications(customer.id)
            return [customer.id, apps]
          } catch {
            return [customer.id, []]
          }
        }),
      )
      setApplicationsByCustomer(Object.fromEntries(entries))
    } catch (err) {
      setError(formatApiError(err))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadCustomers()
  }, [loadCustomers])

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  function fillSampleData() {
    setForm(sampleCustomer())
    setError(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const payload = {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || null,
      }
      await createCustomer(payload)
      setForm(emptyForm)
      await loadCustomers()
    } catch (err) {
      setError(formatApiError(err))
    } finally {
      setSubmitting(false)
    }
  }

  async function startApplication(customerId, workflowKey) {
    const createKey = `${customerId}:${workflowKey}`
    setCreatingFor(createKey)
    setError(null)
    try {
      const created = await createApplication({ customerId, workflowKey })
      navigate(`/portal/applications/${created.id}/edit`)
    } catch (err) {
      setError(formatApiError(err))
      setCreatingFor(null)
    }
  }

  return (
    <div className="customers-page">
      <h1>Customers</h1>
      <p className="page-intro">
        Select or create a customer before starting a policy application.
      </p>

      <div className="alert alert-warning demo-boundary" role="note">
        <strong>Shared public demo — use synthetic data only.</strong>
        <p>
          Do not enter real personal information. This portal has no sign-in;
          data may be visible to other visitors. Prefer the sample-data button
          below.
        </p>
      </div>

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}

      <section className="panel">
        <h2>Create customer</h2>
        <form className="customer-form" onSubmit={handleSubmit}>
          <label htmlFor="firstName">
            First name
            <input
              id="firstName"
              name="firstName"
              value={form.firstName}
              onChange={handleChange}
              required
            />
          </label>
          <label htmlFor="lastName">
            Last name
            <input
              id="lastName"
              name="lastName"
              value={form.lastName}
              onChange={handleChange}
              required
            />
          </label>
          <label htmlFor="email">
            Email
            <input
              id="email"
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              required
            />
          </label>
          <label htmlFor="phone">
            Phone (optional)
            <input
              id="phone"
              name="phone"
              type="tel"
              value={form.phone}
              onChange={handleChange}
            />
          </label>
          <div className="form-actions inline-actions">
            <button type="button" className="button" onClick={fillSampleData}>
              Fill sample data
            </button>
            <button type="submit" className="button button-primary" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create customer'}
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <h2>Customer list</h2>
        {loading ? (
          <p>Loading customers…</p>
        ) : customers.length === 0 ? (
          <p>No customers yet. Create one with synthetic data to begin.</p>
        ) : (
          <div className="customer-cards">
            {customers.map((customer) => {
              const apps = applicationsByCustomer[customer.id] || []
              return (
                <article key={customer.id} className="customer-card">
                  <header>
                    <h3>
                      {customer.firstName} {customer.lastName}
                    </h3>
                    <p>
                      {customer.email}
                      {customer.phone ? ` · ${customer.phone}` : ''}
                    </p>
                    <p className="muted">
                      Created {new Date(customer.createdAt).toLocaleString()}
                    </p>
                  </header>

                  <div className="customer-start-actions">
                    {WORKFLOW_OPTIONS.map((option) => {
                      const createKey = `${customer.id}:${option.key}`
                      return (
                        <button
                          key={option.key}
                          type="button"
                          className="button button-primary"
                          disabled={Boolean(creatingFor)}
                          onClick={() => startApplication(customer.id, option.key)}
                        >
                          {creatingFor === createKey
                            ? 'Starting…'
                            : `New ${option.label}`}
                        </button>
                      )
                    })}
                  </div>

                  <div className="customer-applications">
                    <h4>Applications</h4>
                    {apps.length === 0 ? (
                      <p className="muted">No applications yet.</p>
                    ) : (
                      <ul className="application-list">
                        {apps.map((app) => (
                          <li key={app.id}>
                            <span>
                              <code>{app.id.slice(0, 8)}</code>…{' '}
                              {workflowLabel(app.workflowKey)} v{app.workflowVersion}{' '}
                              — {app.status}
                            </span>
                            {app.status === 'DRAFT' ? (
                              <Link
                                to={`/portal/applications/${app.id}/edit`}
                                className="button button-primary"
                              >
                                Resume
                              </Link>
                            ) : (
                              <Link
                                to={`/portal/applications/${app.id}`}
                                className="button"
                              >
                                View
                              </Link>
                            )}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}
