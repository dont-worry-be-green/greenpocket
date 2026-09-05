import client from './client'

export function getDiagnosis(params = {}) {
  return client.get('/diagnosis', { params })
}

export function getBillTargetMonth() {
  return client.get('/bills/target-month')
}

export function checkBillDuplicates({ billingMonth, utilityTypes }) {
  return client.get('/bills/duplicate-check', {
    params: { billingMonth, utilityTypes: utilityTypes.join(',') },
  })
}

export function createBill(payload) {
  return client.post('/bills', payload)
}
