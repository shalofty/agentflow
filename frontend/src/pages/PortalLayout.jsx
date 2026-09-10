import { Link, Outlet } from 'react-router-dom'
import { ApiReadyProvider, useApiReady } from '../api/ApiReadyContext'

function PortalGate() {
  const { status, error, retry } = useApiReady()

  if (status === 'ready') {
    return <Outlet />
  }

  return (
    <div className="api-ready-panel panel">
      <h1>Connecting to AgentFlow API</h1>
      {status === 'checking' && (
        <p>Checking API health…</p>
      )}
      {status === 'waking' && (
        <p>
          The free API host may be waking from idle. This can take up to a
          minute. Waiting safely — mutations are not retried automatically.
        </p>
      )}
      {status === 'failed' && (
        <>
          <div className="alert alert-error" role="alert">
            {error || 'Unable to reach the API.'}
          </div>
          <p>
            You can retry the read-only health check. Do not resubmit forms until
            the API is ready.
          </p>
          <button type="button" className="button button-primary" onClick={retry}>
            Retry health check
          </button>
        </>
      )}
      {(status === 'checking' || status === 'waking') && (
        <p className="muted" aria-live="polite">
          Status: {status}
        </p>
      )}
    </div>
  )
}

export default function PortalLayout() {
  return (
    <ApiReadyProvider>
      <div className="portal">
        <header className="portal-header">
          <div className="portal-header-inner">
            <Link to="/portal/customers" className="portal-brand">
              AgentFlow Portal
            </Link>
            <nav className="portal-nav">
              <Link to="/portal/customers">Customers</Link>
              <Link to="/">Back to landing</Link>
            </nav>
          </div>
        </header>
        <main className="portal-main">
          <div className="alert alert-warning demo-boundary compact" role="note">
            Shared public demo — use synthetic data only.
          </div>
          <PortalGate />
        </main>
      </div>
    </ApiReadyProvider>
  )
}
