function isFieldVisible(field, values) {
  if (!field.visibleWhen) {
    return true
  }
  const { field: dependentField, equals } = field.visibleWhen
  return values[dependentField] === equals
}

function fieldValue(values, field) {
  const raw = values[field.name]
  if (field.type === 'checkbox') {
    return Boolean(raw)
  }
  return raw ?? ''
}

function FieldInput({ field, value, onChange, disabled, invalid, describedBy }) {
  const id = `field-${field.name}`
  const common = {
    id,
    name: field.name,
    required: field.required,
    disabled,
    'aria-invalid': invalid || undefined,
    'aria-describedby': describedBy,
    onChange,
  }

  switch (field.type) {
    case 'select':
      return (
        <select {...common} value={value}>
          <option value="">Select…</option>
          {field.options?.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      )
    case 'checkbox':
      return <input {...common} type="checkbox" checked={value} />
    case 'number':
      return <input {...common} type="number" value={value} />
    case 'date':
      return <input {...common} type="date" value={value} />
    case 'text':
    default:
      return <input {...common} type="text" value={value} />
  }
}

export default function DynamicForm({
  definition,
  values,
  onChange,
  fieldErrors = {},
  readOnly = false,
  formId = 'application-form',
}) {
  const fields = definition?.fields ?? []

  function handleChange(event) {
    if (readOnly) {
      return
    }
    const { name, type, value, checked } = event.target
    const nextValue = type === 'checkbox' ? checked : value
    onChange({ ...values, [name]: nextValue })
  }

  return (
    <form
      id={formId}
      className="dynamic-form"
      onSubmit={(event) => event.preventDefault()}
      noValidate
    >
      {fields.map((field) => {
        if (!isFieldVisible(field, values)) {
          return null
        }

        const value = fieldValue(values, field)
        const error = fieldErrors[field.name]
        const errorId = error ? `field-${field.name}-error` : undefined
        const inputId = `field-${field.name}`

        if (field.type === 'checkbox') {
          return (
            <div key={field.name} className="form-field checkbox-field">
              <div className="checkbox-row">
                <FieldInput
                  field={field}
                  value={value}
                  onChange={handleChange}
                  disabled={readOnly}
                  invalid={Boolean(error)}
                  describedBy={errorId}
                />
                <label htmlFor={inputId}>
                  {field.label}
                  {field.required ? (
                    <span className="required-marker" aria-hidden="true">
                      {' '}
                      *
                    </span>
                  ) : (
                    <span className="optional-marker"> (optional)</span>
                  )}
                </label>
              </div>
              {error && (
                <p id={errorId} className="field-error" role="alert">
                  {error}
                </p>
              )}
            </div>
          )
        }

        return (
          <div key={field.name} className="form-field">
            <label htmlFor={inputId}>
              {field.label}
              {field.required ? (
                <span className="required-marker" aria-hidden="true">
                  {' '}
                  *
                </span>
              ) : (
                <span className="optional-marker"> (optional)</span>
              )}
            </label>
            <FieldInput
              field={field}
              value={value}
              onChange={handleChange}
              disabled={readOnly}
              invalid={Boolean(error)}
              describedBy={errorId}
            />
            {error && (
              <p id={errorId} className="field-error" role="alert">
                {error}
              </p>
            )}
          </div>
        )
      })}
    </form>
  )
}
