import { buildPolicyList, findPolicy } from '@/fixtures/policy'

import client, { ApiError } from './client'
import { isFixtureMode } from './dataSource'

const fake = async (value) => {
  await new Promise((resolve) => setTimeout(resolve, 220))
  return typeof value === 'function' ? value() : value
}

export function getPolicyRecommendations(params = {}) {
  if (isFixtureMode()) return fake(() => buildPolicyList(params))
  return client.get('/policies/recommendations', { params })
}

export function previewPolicyRecommendations(payload) {
  if (isFixtureMode()) return fake(() => buildPolicyList(payload, true))
  return client.post('/policies/recommendations/preview', payload)
}

export function getPolicies(params = {}) {
  if (isFixtureMode()) return fake(() => buildPolicyList(params))
  return client.get('/policies', { params })
}

export function getPolicy(policyId) {
  if (isFixtureMode()) {
    return fake(() => {
      const policy = findPolicy(policyId)
      if (policy) return policy
      throw new ApiError({ code: 'YOUTH_POLICY_NOT_FOUND', message: '청년정책을 찾을 수 없어요.', status: 404 })
    })
  }
  return client.get(`/policies/${encodeURIComponent(policyId)}`)
}
