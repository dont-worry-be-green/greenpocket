import client from './client'

export function getPocketHome() {
  return client.get('/pocket')
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

export function getWithdrawals(params = {}) {
  return client.get('/pocket/withdrawals', { params })
}

export function requestWithdrawal(payload, idempotencyKey) {
  return client.post('/pocket/withdrawals', payload, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}
