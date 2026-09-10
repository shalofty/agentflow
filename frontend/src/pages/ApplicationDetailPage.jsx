import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { formatApiError, getActivities, getApplication } from '../api/client'

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

function ActivityCard({ activity }) {
  const [open, setOpen] = useState(Boolean(activity.errorMessage))
  const detailsId = `activity-${activity.id}-details`

  return (
    <article className="activity-card">
      <button
        type="button"
        className="activity-card-toggle"
        aria-expanded={open}
        aria-controls={detailsId}
        onClick={() => setOpen((value) => !value)}
      >
        <span className="activity-card-summary">
          <span className={statusClass(activity.status)}>{activity.status}</span>
          <strong>{activity.integration}</strong>
          <span className="muted">
            {activity.integrationType} · attempt {activity.attempt} ·{' '}
            {activity.durationMs} ms
          </span>
        </span>
        <span className="activity-card-chevron">{open ? 'Hide' : 'Details'}</span>
      </button>
      {open && (
        <div id={detailsId} className="activity-card-body">
          <dl className="detail-grid">
            <div>
              <dt>Timestamp</dt>
              <dd>{formatTimestamp(activity.createdAt)}</dd>
            </div>
            <div>
              <dt>HTTP / SOAP</dt>
              <dd>{formatProtocolInfo(activity)}</dd>
            </div>
            <div>
              <dt>Action</dt>
              <dd>{activity.action || '—'}</dd>
            </div>
            <div>
              <dt>Correlation</dt>
              <dd>{activity.correlationId || '—'}</dd>
            </div>
          </dl>
          {activity.errorMessage && (
            <pre className="activity-error-block">{activity.errorMessage}</pre>
          )}
          {(activity.requestSummary || activity.responseSummary) &&
            activity.integrationType === 'REST' && (
              <p className="muted">
                {activity.requestSummary || ''}
                {activity.responseSummary ? ` → ${activity.responseSummary}` : ''}
              </p>
            )}
        </div>
      )}
    </article>
  )
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
          setError(formatApiError(err))
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

      <section className="panel" id="integration-activity">
        <h2>Integration activity</h2>
        {activities.length === 0 ? (
          <p>No integration activity yet.</p>
        ) : (
          <div className="activity-list">
            {activities.map((activity) => (
              <ActivityCard key={activity.id} activity={activity} />
            ))}
          </div>
        )}
      </section>

      <div className="form-actions">
        {application.status === 'DRAFT' && (
          <Link
            to={`/portal/applications/${application.id}/edit`}
            className="button button-primary"
          >
            Resume draft
          </Link>
        )}
        <Link to="/portal/customers" className="button">
          Back to customers
        </Link>
      </div>
    </div>
  )
}
