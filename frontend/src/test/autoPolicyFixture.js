export const autoFixture = {
  workflowKey: 'auto-policy',
  title: 'New Auto Policy',
  version: 1,
  fields: [
    {
      name: 'vin',
      label: 'VIN',
      type: 'text',
      required: true,
    },
    {
      name: 'vehicleYear',
      label: 'Vehicle Year',
      type: 'number',
      required: true,
    },
    {
      name: 'coverageType',
      label: 'Coverage Type',
      type: 'select',
      required: true,
      options: [
        { value: 'LIABILITY', label: 'Liability' },
        { value: 'FULL', label: 'Full Coverage' },
      ],
    },
    {
      name: 'hasGarage',
      label: 'Vehicle kept in garage',
      type: 'checkbox',
      required: false,
    },
    {
      name: 'effectiveDate',
      label: 'Effective Date',
      type: 'date',
      required: true,
    },
    {
      name: 'coverageAmount',
      label: 'Coverage Amount',
      type: 'number',
      required: true,
      visibleWhen: { field: 'coverageType', equals: 'FULL' },
    },
  ],
}
