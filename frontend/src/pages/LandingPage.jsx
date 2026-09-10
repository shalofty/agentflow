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
            <li>Frontend: Vite + React (JavaScript)</li>
            <li>Backend: Java, Spring Boot, Maven</li>
            <li>Database: PostgreSQL with Flyway migrations</li>
            <li>Integrations: REST (WebClient) + SOAP (Spring-WS)</li>
            <li>Mocks: Docker Compose services for external systems</li>
            <li>Tooling: Python CSV import, Postman collection</li>
          </ul>
        </section>

        <section>
          <h2>Architecture</h2>
          <p>
            A React SPA talks to a Spring Boot API over REST/JSON. The API
            orchestrates customer and application workflows, persists state in
            PostgreSQL, and calls outbound REST and SOAP mock services.
            Workflow definitions are versioned JSON seeded into the database
            and rendered by a shared form component (coming in later tasks).
          </p>
        </section>

        <section>
          <h2>How to demo</h2>
          <ol>
            <li>Start PostgreSQL and mocks via Docker Compose.</li>
            <li>Run the Spring Boot API on port 8080.</li>
            <li>Run this frontend with <code>npm run dev</code> (port 5173).</li>
            <li>Enter the portal, create a customer, then start a policy application.</li>
            <li>Submit and inspect Integration Activity for troubleshooting.</li>
          </ol>
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
