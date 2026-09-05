import client from './client'

export function getDiagnosis(params = {}) {
  return client.get('/diagnosis', { params })
}

export function getBillTargetMonth() {
  return client.get('/bills/target-month')
}
