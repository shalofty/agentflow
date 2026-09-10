import { Link } from 'react-router-dom'

export default function LandingPage() {
  return (
    <div className="landing">
      <header className="landing-header">
        <h1>AgentFlow</h1>
        <p className="tagline">Insurance agent implementation portal</p>
      </header>

      <main className="landing-main">
        <section>
          <h2>What it is</h2>
          <p>
            AgentFlow is a portfolio modular monolith that simulates how an
            insurance carrier supports independent agencies. Agencies need
            slightly different forms and integration behavior; AgentFlow uses
            JSON-configured workflows instead of hard-coding every
            implementation.
          </p>
        </section>

        <section>
          <h2>Why it exists</h2>
          <p>
            This project demonstrates skills relevant to a Technical
            Implementation Engineer role: REST and SOAP integrations,
            validation, failure handling, integration observability, and
            JSON-driven web forms. It is intentionally not multi-tenant SaaS
            — the goal is understandable professional engineering over
            unnecessary abstraction.
          </p>
        </section>

        <section>
          <h2>Stack</h2>
          <ul>
            <li>Frontend: Vite + React on Cloudflare Pages (with /api proxy)</li>
            <li>Backend: Java 21, Spring Boot on Render Free</li>
            <li>Database: Neon Free PostgreSQL with Flyway migrations</li>
            <li>Integrations: REST (WebClient) + SOAP (Spring-WS)</li>
            <li>
              External systems: Cloudflare Workers (Customer Verification REST,
              Policy Eligibility SOAP)
            </li>
            <li>Tooling: Python CSV import, Postman collection</li>
          </ul>
        </section>

        <section>
          <h2>Architecture</h2>
          <p>
            This Pages site proxies <code>/api</code> to the Spring Boot API.
            The API orchestrates customer and application workflows, persists
            state in Neon PostgreSQL, and calls outbound REST and SOAP Worker
            mocks with request-scoped demo scenarios and a shared service
            secret. Workflow definitions are versioned JSON seeded into the
            database and rendered by a shared dynamic form component.
          </p>
        </section>

        <section>
          <h2>How to demo</h2>
          <ol>
            <li>
              Enter the portal below. The first request after idle time may wait
              while the free API wakes up — give it a moment if the page seems
              slow.
            </li>
            <li>Create a customer, then start an Auto Policy application.</li>
            <li>Complete the JSON-driven form and submit the application.</li>
            <li>
              Inspect Integration Activity for REST/SOAP calls, retries, and
              outcomes.
            </li>
            <li>
              Optional: use the demo failure controls (when enabled) to inject
              REST 500, timeout, SOAP fault, or manual-review scenarios.
            </li>
          </ol>
          <p className="landing-note">
            Local development still uses Docker Compose Postgres and Node/Spring
            mocks; see the repository README for that path.
          </p>
        </section>

        <div className="landing-cta">
          <Link to="/portal/customers" className="button button-primary">
            Enter portal
          </Link>
        </div>
      </main>
    </div>
  )
}
