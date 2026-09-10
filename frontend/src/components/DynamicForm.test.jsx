import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import DynamicForm from './DynamicForm'
import { autoFixture } from '../test/autoPolicyFixture'
import { homeFixture } from '../test/homePolicyFixture'

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

  it('renders home policy fields from fixture without a dedicated form component', () => {
    render(
      <DynamicForm
        definition={homeFixture}
        values={{ occupancyType: 'TENANT' }}
        onChange={() => {}}
      />,
    )
    expect(screen.getByLabelText(/Property Address/i)).toBeRequired()
    expect(screen.getByLabelText(/Dwelling Value/i)).toBeRequired()
    expect(screen.getByLabelText(/Occupancy Type/i)).toBeRequired()
    expect(screen.queryByLabelText(/Replacement Cost/i)).toBeNull()
  })

  it('shows replacementCost when occupancyType is OWNER_OCCUPIED', () => {
    render(
      <DynamicForm
        definition={homeFixture}
        values={{ occupancyType: 'OWNER_OCCUPIED' }}
        onChange={() => {}}
      />,
    )
    expect(screen.getByLabelText(/Replacement Cost/i)).toBeRequired()
  })
})
