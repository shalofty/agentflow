import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getActivities, getApplication } from '../api/client'

function formatError(error) {
  if (error.problem?.errors) {
    const fields = Object.entries(error.problem.errors)
      .map(([field, message]) => `${field}: ${message}`)
      .join('; ')
    return `${error.message} (${fields})`
  }
  return error.message
}

function formatTimestamp(value) {
  if (!value) {
    return '—'
  }
  return new Date(value).toLocaleString()
}

function formatProtocolInfo(activity) {
  if (activity.integrationType === 'REST') {
    return activity.httpStatus != null ? `HTTP ${activity.httpStatus}` : '—'
  }
  const parts = []
  if (activity.requestSummary) {
    parts.push(`Req: ${activity.requestSummary}`)
  }
  if (activity.responseSummary) {
    parts.push(`Resp: ${activity.responseSummary}`)
  }
  return parts.length > 0 ? parts.join(' · ') : '—'
}

function statusClass(status) {
  const variants = {
    SUCCESS: 'status-success',
    APPROVED: 'status-success',
    FAILED: 'status-failed',
    INTEGRATION_FAILURE: 'status-failed',
    INELIGIBLE: 'status-failed',
    MANUAL_REVIEW: 'status-warning',
    DRAFT: 'status-neutral',
    SUBMITTED: 'status-info',
    CUSTOMER_VERIFICATION: 'status-info',
    ELIGIBILITY_CHECK: 'status-info',
  }
  return `status-badge ${variants[status] ?? 'status-neutral'}`
}

export default function ApplicationDetailPage() {
  const { id } = useParams()
  const [application, setApplication] = useState(null)
  const [activities, setActivities] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [app, timeline] = await Promise.all([
          getApplication(id),
          getActivities(id),
        ])
        if (!cancelled) {
          setApplication(app)
          setActivities(timeline)
        }
      } catch (err) {
        if (!cancelled) {
          setError(formatError(err))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    load()

    return () => {
      cancelled = true
    }
  }, [id])

  if (loading) {
    return <p>Loading application…</p>
  }

  if (error) {
    return (
      <div>
        <div className="alert alert-error" role="alert">
          {error}
        </div>
        <Link to="/portal/customers" className="button">
          Back to customers
        </Link>
      </div>
    )
  }

  return (
    <div className="application-detail-page">
      <h1>Application detail</h1>
      <p className="page-intro">
        {application.workflowKey} v{application.workflowVersion} —{' '}
        <span className={statusClass(application.status)}>{application.status}</span>
      </p>

      <section className="panel">
        <h2>Summary</h2>
        <dl className="detail-grid">
          <div>
            <dt>Application ID</dt>
            <dd>
              <code>{application.id}</code>
            </dd>
          </div>
          <div>
            <dt>Customer ID</dt>
            <dd>
              <code>{application.customerId}</code>
            </dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>
              <span className={statusClass(application.status)}>{application.status}</span>
            </dd>
          </div>
          <div>
            <dt>Correlation ID</dt>
            <dd>{application.correlationId || '—'}</dd>
          </div>
          <div>
            <dt>Submitted</dt>
            <dd>{formatTimestamp(application.submittedAt)}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{formatTimestamp(application.createdAt)}</dd>
          </div>
        </dl>
      </section>

      <section className="panel">
        <h2>Integration activity</h2>
        {activities.length === 0 ? (
          <p>No integration activity yet.</p>
        ) : (
          <div className="table-scroll">
            <table className="data-table activity-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Integration</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Attempt</th>
                  <th>HTTP / SOAP</th>
                  <th>Duration</th>
                  <th>Error</th>
                </tr>
              </thead>
              <tbody>
                {activities.map((activity) => (
                  <tr key={activity.id}>
                    <td>{formatTimestamp(activity.createdAt)}</td>
                    <td>{activity.integration}</td>
                    <td>{activity.integrationType}</td>
                    <td>
                      <span className={statusClass(activity.status)}>{activity.status}</span>
                    </td>
                    <td>{activity.attempt}</td>
                    <td>{formatProtocolInfo(activity)}</td>
                    <td>{activity.durationMs} ms</td>
                    <td className="error-cell">{activity.errorMessage || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="form-actions">
        <Link to="/portal/customers" className="button">
          Back to customers
        </Link>
      </div>
    </div>
  )
}
