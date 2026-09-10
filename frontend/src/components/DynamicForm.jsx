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

function FieldInput({ field, value, onChange }) {
  const id = `field-${field.name}`

  switch (field.type) {
    case 'select':
      return (
        <select
          id={id}
          name={field.name}
          value={value}
          required={field.required}
          onChange={onChange}
        >
          <option value="">Select…</option>
          {field.options?.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      )
    case 'checkbox':
      return (
        <input
          id={id}
          name={field.name}
          type="checkbox"
          checked={value}
          required={field.required}
          onChange={onChange}
        />
      )
    case 'number':
      return (
        <input
          id={id}
          name={field.name}
          type="number"
          value={value}
          required={field.required}
          onChange={onChange}
        />
      )
    case 'date':
      return (
        <input
          id={id}
          name={field.name}
          type="date"
          value={value}
          required={field.required}
          onChange={onChange}
        />
      )
    case 'text':
    default:
      return (
        <input
          id={id}
          name={field.name}
          type="text"
          value={value}
          required={field.required}
          onChange={onChange}
        />
      )
  }
}

export default function DynamicForm({ definition, values, onChange }) {
  const fields = definition?.fields ?? []

  function handleChange(event) {
    const { name, type, value, checked } = event.target
    const nextValue = type === 'checkbox' ? checked : value
    onChange({ ...values, [name]: nextValue })
  }

  return (
    <form className="dynamic-form" onSubmit={(event) => event.preventDefault()}>
      {fields.map((field) => {
        if (!isFieldVisible(field, values)) {
          return null
        }

        const value = fieldValue(values, field)

        return (
          <label key={field.name} htmlFor={`field-${field.name}`}>
            {field.label}
            <FieldInput field={field} value={value} onChange={handleChange} />
          </label>
        )
      })}
    </form>
  )
}
