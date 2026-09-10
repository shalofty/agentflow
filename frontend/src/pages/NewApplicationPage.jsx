import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { createApplication, getApplicationDefinition } from '../api/client'
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

export default function NewApplicationPage() {
  const [searchParams] = useSearchParams()
  const customerId = searchParams.get('customerId')
  const workflowKey = searchParams.get('workflowKey') ?? 'auto-policy'

  const [application, setApplication] = useState(null)
  const [definition, setDefinition] = useState(null)
  const [values, setValues] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

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

      <section className="panel">
        <DynamicForm
          definition={definition}
          values={values}
          onChange={setValues}
        />
      </section>

      <Link to="/portal/customers" className="button">
        Back to customers
      </Link>
    </div>
  )
}
