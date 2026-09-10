/** Focus the first invalid field after a validation failure. */
export function focusFirstInvalidField(fieldErrors, definition) {
  const fields = definition?.fields ?? []
  const first = fields.find((field) => fieldErrors[field.name])
  if (!first) {
    return
  }
  const el = document.getElementById(`field-${first.name}`)
  if (el && typeof el.focus === 'function') {
    el.focus()
  }
}
