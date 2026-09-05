import client from './client'

export function getPocketHome() {
  return client.get('/pocket')
}

export function getPocketBalance() {
  return client.get('/pocket/balance')
}

export function getConvertibleMileage() {
  return client.get('/pocket/convertible-mileage')
}

export function getPocketTransactions(params = {}) {
  return client.get('/pocket/transactions', { params })
}

export function getPocketManagement() {
  return client.get('/pocket/management')
}

export function getWithdrawalAccounts() {
  return client.get('/pocket/accounts')
}

export function createWithdrawalAccount(payload) {
  return client.post('/pocket/accounts', payload)
}

export function setDefaultWithdrawalAccount(accountId) {
  return client.put(`/pocket/accounts/${accountId}/default`)
}

export function getWithdrawals(params = {}) {
  return client.get('/pocket/withdrawals', { params })
}

export function requestWithdrawal(payload, idempotencyKey) {
  return client.post('/pocket/withdrawals', payload, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export function startMileageConversion(payload) {
  return client.post('/pocket/conversions', payload)
}

export function completeMileageConversion(conversionId, idempotencyKey) {
  return client.post(`/pocket/conversions/${conversionId}/complete`, null, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}
