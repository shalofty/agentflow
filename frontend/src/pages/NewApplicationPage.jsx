import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import {
  createApplication,
  getDemoConfiguration,
  getApplicationDefinition,
  saveApplicationData,
  submitApplication,
} from '../api/client'
import DynamicForm from '../components/DynamicForm'

function formatError(error) {
  if (error.problem?.errors) {
    const fields = Object.entries(error.problem.errors)
      .map(([field, message]) => `${field}: ${message}`)
      .join('; ')
    return `${error.message} (${fields})`
  }
  return error.message
}

const WORKFLOW_OPTIONS = [
  { key: 'auto-policy', label: 'Auto' },
  { key: 'home-policy', label: 'Home' },
]

export default function NewApplicationPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const customerId = searchParams.get('customerId')
  const workflowKey = searchParams.get('workflowKey') ?? 'auto-policy'

  function handleWorkflowChange(event) {
    const nextWorkflowKey = event.target.value
    const params = new URLSearchParams(searchParams)
    params.set('workflowKey', nextWorkflowKey)
    navigate(`/portal/applications/new?${params.toString()}`, { replace: true })
  }

  const [application, setApplication] = useState(null)
  const [definition, setDefinition] = useState(null)
  const [values, setValues] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saveError, setSaveError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [saveMessage, setSaveMessage] = useState(null)
  const [demoConfiguration, setDemoConfiguration] = useState(null)
  const [demoScenario, setDemoScenario] = useState('')

  function handleValuesChange(nextValues) {
    setValues(nextValues)
    setSaveError(null)
  }

  useEffect(() => {
    let cancelled = false
    getDemoConfiguration()
      .then((configuration) => {
        if (!cancelled) {
          setDemoConfiguration(configuration)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDemoConfiguration({ enabled: false, scenarios: [] })
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!customerId) {
      setError('Missing customer. Select a customer from the list first.')
      setLoading(false)
      return
    }

    let cancelled = false

    async function startApplication() {
      setLoading(true)
      setError(null)
      try {
        const created = await createApplication({ customerId, workflowKey })
        if (cancelled) {
          return
        }
        const pinnedDefinition = await getApplicationDefinition(created.id)
        if (cancelled) {
          return
        }
        setApplication(created)
        setDefinition(pinnedDefinition.definitionJson)
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

    startApplication()

    return () => {
      cancelled = true
    }
  }, [customerId, workflowKey])

  async function handleSaveDraft() {
    if (!application) {
      return
    }
    setSaving(true)
    setSaveMessage(null)
    setSaveError(null)
    try {
      const updated = await saveApplicationData(application.id, values)
      setApplication(updated)
      setSaveMessage('Draft saved.')
      setSaveError(null)
    } catch (err) {
      setSaveError(formatError(err))
    } finally {
      setSaving(false)
    }
  }

  async function handleSubmit() {
    if (!application) {
      return
    }
    setSubmitting(true)
    setSaveMessage(null)
    setSaveError(null)
    try {
      await saveApplicationData(application.id, values)
      const submitted = await submitApplication(application.id, demoScenario)
      setApplication(submitted)
      setSaveMessage(`Application submitted: ${submitted.status}. View integration activity below.`)
    } catch (err) {
      setSaveError(formatError(err))
    } finally {
      setSubmitting(false)
    }
  }

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
    <div className="new-application-page">
      <h1>{definition?.title ?? 'New Application'}</h1>
      <p className="page-intro">
        Application {application.id.slice(0, 8)}… — {application.workflowKey}{' '}
        v{application.workflowVersion} ({application.status})
      </p>

      <section className="panel workflow-picker">
        <label htmlFor="workflow-key">Policy type</label>
        <select
          id="workflow-key"
          value={workflowKey}
          onChange={handleWorkflowChange}
          disabled={application.status !== 'DRAFT'}
        >
          {WORKFLOW_OPTIONS.map((option) => (
            <option key={option.key} value={option.key}>
              {option.label}
            </option>
          ))}
        </select>
      </section>

      {saveMessage && (
        <div className="alert alert-success" role="status">
          {saveMessage}{' '}
          {application.status !== 'DRAFT' && (
            <Link to={`/portal/applications/${application.id}`}>
              View application detail
            </Link>
          )}
        </div>
      )}

      {saveError && (
        <div className="alert alert-error" role="alert">
          {saveError}
        </div>
      )}

      <section className="panel">
        <DynamicForm
          definition={definition}
          values={values}
          onChange={handleValuesChange}
        />
      </section>

      {demoConfiguration?.enabled && (
        <section className="panel">
          <h2>Demo failure injection</h2>
          <label htmlFor="demo-scenario">Submit scenario</label>
          <select
            id="demo-scenario"
            value={demoScenario}
            onChange={(event) => setDemoScenario(event.target.value)}
          >
            <option value="">Normal integrations</option>
            {demoConfiguration.scenarios.map((scenario) => (
              <option key={scenario} value={scenario}>
                {scenario}
              </option>
            ))}
          </select>
        </section>
      )}

      <div className="form-actions">
        <button
          type="button"
          className="button button-primary"
          onClick={handleSaveDraft}
          disabled={saving}
        >
          {saving ? 'Saving…' : 'Save draft'}
        </button>
        <button
          type="button"
          className="button button-primary"
          onClick={handleSubmit}
          disabled={saving || submitting || application.status !== 'DRAFT'}
        >
          {submitting ? 'Submitting…' : 'Submit application'}
        </button>
        <Link to="/portal/customers" className="button">
          Back to customers
        </Link>
      </div>
    </div>
  )
}
