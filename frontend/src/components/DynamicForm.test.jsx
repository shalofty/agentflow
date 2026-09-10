import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import DynamicForm from './DynamicForm'
import { autoFixture } from '../test/autoPolicyFixture'

describe('DynamicForm', () => {
  it('hides coverageAmount unless FULL', () => {
    render(
      <DynamicForm
        definition={autoFixture}
        values={{ coverageType: 'LIABILITY' }}
        onChange={() => {}}
      />,
    )
    expect(screen.queryByLabelText(/Coverage Amount/i)).toBeNull()
  })

  it('shows coverageAmount when coverageType is FULL', () => {
    render(
      <DynamicForm
        definition={autoFixture}
        values={{ coverageType: 'FULL' }}
        onChange={() => {}}
      />,
    )
    expect(screen.getByLabelText(/Coverage Amount/i)).toBeRequired()
  })

  it('marks required fields with the required attribute', () => {
    render(
      <DynamicForm definition={autoFixture} values={{}} onChange={() => {}} />,
    )
    expect(screen.getByLabelText(/^VIN$/i)).toBeRequired()
    expect(screen.getByLabelText(/Vehicle Year/i)).toBeRequired()
    expect(screen.getByLabelText(/Coverage Type/i)).toBeRequired()
    expect(screen.getByLabelText(/Effective Date/i)).toBeRequired()
    expect(screen.getByLabelText(/Vehicle kept in garage/i)).not.toBeRequired()
  })
})
