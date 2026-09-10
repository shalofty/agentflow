import { Link, Outlet } from 'react-router-dom'

export default function PortalLayout() {
  return (
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
        <Outlet />
      </main>
    </div>
  )
}
