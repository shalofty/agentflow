import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createCustomer, listCustomers } from '../api/client'

const emptyForm = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
}

function formatError(error) {
  if (error.problem?.errors) {
    const fields = Object.entries(error.problem.errors)
      .map(([field, message]) => `${field}: ${message}`)
      .join('; ')
    return `${error.message} (${fields})`
  }
  return error.message
}

export default function CustomersPage() {
  const [customers, setCustomers] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const loadCustomers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listCustomers()
      setCustomers(data)
    } catch (err) {
      setError(formatError(err))
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
      setError(formatError(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="customers-page">
      <h1>Customers</h1>
      <p className="page-intro">
        Select or create a customer before starting a policy application.
      </p>

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}

      <section className="panel">
        <h2>Create customer</h2>
        <form className="customer-form" onSubmit={handleSubmit}>
          <label>
            First name
            <input
              name="firstName"
              value={form.firstName}
              onChange={handleChange}
              required
            />
          </label>
          <label>
            Last name
            <input
              name="lastName"
              value={form.lastName}
              onChange={handleChange}
              required
            />
          </label>
          <label>
            Email
            <input
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              required
            />
          </label>
          <label>
            Phone
            <input
              name="phone"
              type="tel"
              value={form.phone}
              onChange={handleChange}
            />
          </label>
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create customer'}
          </button>
        </form>
      </section>

      <section className="panel">
        <h2>Customer list</h2>
        {loading ? (
          <p>Loading customers…</p>
        ) : customers.length === 0 ? (
          <p>No customers yet.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {customers.map((customer) => (
                <tr key={customer.id}>
                  <td>
                    {customer.firstName} {customer.lastName}
                  </td>
                  <td>{customer.email}</td>
                  <td>{customer.phone || '—'}</td>
                  <td>{new Date(customer.createdAt).toLocaleString()}</td>
                  <td>
                    <Link
                      to={`/portal/applications/new?customerId=${customer.id}&workflowKey=auto-policy`}
                      className="button button-primary"
                    >
                      New Auto Policy
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  )
}
