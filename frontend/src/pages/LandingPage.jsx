import { Link } from 'react-router-dom'

const REPO_URL = 'https://github.com/shalofty/agentflow'

export default function LandingPage() {
  return (
    <div className="landing">
      <header className="landing-header">
        <h1>AgentFlow</h1>
        <p className="tagline">Insurance agent implementation portal</p>
        <div className="landing-cta landing-cta-top">
          <Link to="/portal/customers" className="button button-primary">
            Try demo
          </Link>
          <a
            href={REPO_URL}
            className="button"
            target="_blank"
            rel="noreferrer"
          >
            View source
          </a>
        </div>
        <p className="landing-note">
          Shared public demo — use synthetic data only. Free-tier API may take
          up to a minute to wake after idle.
        </p>
      </header>

      <main className="landing-main">
        <section>
          <h2>Expected walkthrough</h2>
          <ol>
            <li>Enter the portal and create a synthetic customer (or fill sample data).</li>
            <li>Start an Auto Policy, save a draft, reload, and resume the same application.</li>
            <li>Submit and inspect REST + SOAP integration activity on the detail page.</li>
            <li>Optional: start a Home Policy and inject a SOAP fault to see retries.</li>
          </ol>
        </section>

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
            Browser → Cloudflare Pages (<code>/api</code> proxy) → Render Free
            Spring Boot API → Neon PostgreSQL, with outbound calls from the API
            to Cloudflare REST and SOAP Workers. Workflow definitions are
            versioned JSON seeded into the database and rendered by a shared
            dynamic form component.
          </p>
        </section>

        <section>
          <h2>How to demo</h2>
          <ol>
            <li>
              Use <strong>Try demo</strong> above. The first request after idle
              time may wait while the free API wakes up.
            </li>
            <li>Create a synthetic customer, then start an Auto Policy application.</li>
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
            Try demo
          </Link>
          <a
            href={REPO_URL}
            className="button"
            target="_blank"
            rel="noreferrer"
          >
            View source
          </a>
        </div>
      </main>
    </div>
  )
}
