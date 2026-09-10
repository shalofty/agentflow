import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  fieldErrorsFromProblem,
  formatApiError,
  getApplication,
  getApplicationData,
  getApplicationDefinition,
  getDemoConfiguration,
  saveApplicationData,
  submitApplication,
} from '../api/client'
import DynamicForm from '../components/DynamicForm'
import { focusFirstInvalidField } from '../components/focusFirstInvalidField'

const WORKFLOW_LABELS = {
  'auto-policy': 'Auto Policy',
  'home-policy': 'Home Policy',
}

function outcomePresentation(status) {
  switch (status) {
    case 'APPROVED':
      return {
        className: 'alert alert-success',
        title: 'Application approved',
        detail: 'Customer verification and policy eligibility both succeeded.',
      }
    case 'MANUAL_REVIEW':
      return {
        className: 'alert alert-warning',
        title: 'Manual review required',
        detail: 'Submission completed, but eligibility needs human review.',
      }
    case 'INELIGIBLE':
      return {
        className: 'alert alert-error',
        title: 'Application ineligible',
        detail: 'Submission completed; eligibility returned an ineligible result.',
      }
    case 'INTEGRATION_FAILURE':
      return {
        className: 'alert alert-error',
        title: 'Integration failure',
        detail:
          'The submission was processed, but a downstream integration failed after retries.',
      }
    case 'SUBMITTED':
    case 'CUSTOMER_VERIFICATION':
    case 'ELIGIBILITY_CHECK':
      return {
        className: 'alert alert-info',
        title: 'Submission in progress',
        detail: `Current status: ${status.replaceAll('_', ' ').toLowerCase()}.`,
      }
    default:
      return {
        className: 'alert alert-info',
        title: 'Submission processed',
        detail: `Application status is ${status}.`,
      }
  }
}

export default function EditApplicationPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [application, setApplication] = useState(null)
  const [definition, setDefinition] = useState(null)
  const [values, setValues] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [bannerError, setBannerError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [draftSaved, setDraftSaved] = useState(false)
  const [submittedOutcome, setSubmittedOutcome] = useState(null)
  const [demoConfiguration, setDemoConfiguration] = useState(null)
  const [demoScenario, setDemoScenario] = useState('')

  const isDraft = application?.status === 'DRAFT'
  const busy = saving || submitting
  const readOnly = !isDraft || busy

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
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      setFieldErrors({})
      setBannerError(null)
      setDraftSaved(false)
      setSubmittedOutcome(null)
      try {
        const [app, data, pinned] = await Promise.all([
          getApplication(id),
          getApplicationData(id),
          getApplicationDefinition(id),
        ])
        if (cancelled) {
          return
        }
        setApplication(app)
        setValues(data.payload || {})
        setDefinition(pinned.definitionJson)
        if (app.status !== 'DRAFT') {
          setSubmittedOutcome(outcomePresentation(app.status))
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

  function handleValuesChange(nextValues) {
    setValues(nextValues)
    setFieldErrors({})
    setBannerError(null)
    setDraftSaved(false)
  }

  function applyValidationFailure(err) {
    const nextFieldErrors = fieldErrorsFromProblem(err.problem)
    setFieldErrors(nextFieldErrors)
    setBannerError(formatApiError(err))
    focusFirstInvalidField(nextFieldErrors, definition)
  }

  async function handleSaveDraft() {
    if (!application || !isDraft || busy) {
      return
    }
    setSaving(true)
    setBannerError(null)
    setDraftSaved(false)
    try {
      const updated = await saveApplicationData(application.id, values)
      setApplication(updated)
      setFieldErrors({})
      setDraftSaved(true)
    } catch (err) {
      if (err.problem?.errors) {
        applyValidationFailure(err)
      } else {
        setBannerError(formatApiError(err))
      }
      // Reconcile if the draft was claimed while saving.
      try {
        const latest = await getApplication(application.id)
        setApplication(latest)
        if (latest.status !== 'DRAFT') {
          setSubmittedOutcome(outcomePresentation(latest.status))
        }
      } catch {
        // Keep the original error.
      }
    } finally {
      setSaving(false)
    }
  }

  async function handleSubmit() {
    if (!application || !isDraft || busy) {
      return
    }
    setSubmitting(true)
    setBannerError(null)
    setDraftSaved(false)
    try {
      await saveApplicationData(application.id, values)
      const submitted = await submitApplication(application.id, demoScenario)
      setApplication(submitted)
      setFieldErrors({})
      setSubmittedOutcome(outcomePresentation(submitted.status))
    } catch (err) {
      if (err.problem?.errors) {
        applyValidationFailure(err)
      } else {
        setBannerError(formatApiError(err))
      }
      try {
        const latest = await getApplication(application.id)
        setApplication(latest)
        if (latest.status !== 'DRAFT') {
          setSubmittedOutcome(outcomePresentation(latest.status))
        }
      } catch {
        // Keep the original error.
      }
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

  const workflowLabel =
    WORKFLOW_LABELS[application.workflowKey] || application.workflowKey

  return (
    <div className="edit-application-page">
      <h1>{definition?.title ?? 'Application'}</h1>
      <p className="page-intro">
        Application <code>{application.id.slice(0, 8)}</code>… — {workflowLabel}{' '}
        v{application.workflowVersion} ({application.status})
      </p>

      <section className="panel">
        <h2>Policy type</h2>
        <p className="pinned-workflow">
          {workflowLabel} (pinned version {application.workflowVersion})
        </p>
        <p className="field-hint">
          This draft keeps the workflow version it started with. To start a
          different policy type, return to the customer and create a new
          application — this draft is not abandoned automatically.
        </p>
        <Link to="/portal/customers" className="button">
          Back to customer list
        </Link>
      </section>

      {draftSaved && isDraft && (
        <div className="alert alert-success" role="status">
          Draft saved. You can reload this page or leave and resume from the
          customer list.
        </div>
      )}

      {submittedOutcome && (
        <div className={submittedOutcome.className} role="status">
          <strong>{submittedOutcome.title}</strong>
          <p>{submittedOutcome.detail}</p>
          <div className="alert-actions">
            <Link to={`/portal/applications/${application.id}`}>
              View application detail
            </Link>
            <Link to={`/portal/applications/${application.id}#integration-activity`}>
              View integration activity
            </Link>
          </div>
        </div>
      )}

      {bannerError && (
        <div className="alert alert-error" role="alert">
          {bannerError}
        </div>
      )}

      {!isDraft && !submittedOutcome && (
        <div className="alert alert-info" role="status">
          This application is no longer a draft and cannot be edited.
          <div className="alert-actions">
            <Link to={`/portal/applications/${application.id}`}>
              View application detail
            </Link>
          </div>
        </div>
      )}

      <section className="panel">
        <DynamicForm
          definition={definition}
          values={values}
          onChange={handleValuesChange}
          fieldErrors={fieldErrors}
          readOnly={readOnly}
        />
      </section>

      {demoConfiguration?.enabled && (
        <section className="panel">
          <h2>Demo failure injection</h2>
          <label htmlFor="demo-scenario">Submit scenario</label>
          <select
            id="demo-scenario"
            className="control-select"
            value={demoScenario}
            onChange={(event) => setDemoScenario(event.target.value)}
            disabled={readOnly}
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
        {isDraft && (
          <>
            <button
              type="button"
              className="button button-primary"
              onClick={handleSaveDraft}
              disabled={busy}
            >
              {saving ? 'Saving…' : 'Save draft'}
            </button>
            <button
              type="button"
              className="button button-primary"
              onClick={handleSubmit}
              disabled={busy}
            >
              {submitting ? 'Submitting…' : 'Submit application'}
            </button>
          </>
        )}
        {!isDraft && (
          <Link to={`/portal/applications/${application.id}`} className="button button-primary">
            View application detail
          </Link>
        )}
        <button
          type="button"
          className="button"
          onClick={() => navigate('/portal/customers')}
        >
          Back to customers
        </button>
      </div>
    </div>
  )
}
