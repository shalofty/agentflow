export const homeFixture = {
  workflowKey: 'home-policy',
  title: 'New Home Policy',
  version: 1,
  fields: [
    {
      name: 'address',
      label: 'Property Address',
      type: 'text',
      required: true,
    },
    {
      name: 'dwellingValue',
      label: 'Dwelling Value',
      type: 'number',
      required: true,
    },
    {
      name: 'occupancyType',
      label: 'Occupancy Type',
      type: 'select',
      required: true,
      options: [
        { value: 'OWNER_OCCUPIED', label: 'Owner Occupied' },
        { value: 'TENANT', label: 'Tenant' },
        { value: 'VACANT', label: 'Vacant' },
      ],
    },
    {
      name: 'hasSwimmingPool',
      label: 'Swimming pool on property',
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
      name: 'replacementCost',
      label: 'Replacement Cost',
      type: 'number',
      required: true,
      visibleWhen: { field: 'occupancyType', equals: 'OWNER_OCCUPIED' },
    },
  ],
}
