import { buildProfileResult, SEOUL_SIGUNGUS, SIDOS } from '@/fixtures/onboarding'

import client from './client'
import { isFixtureMode } from './dataSource'

const fake = async (value, ms = 220) => {
  await new Promise((resolve) => setTimeout(resolve, ms))
  return typeof value === 'function' ? value() : value
}

export function getRegions({ sidoCode } = {}) {
  if (isFixtureMode()) {
    if (!sidoCode) return fake({ level: 'SIDO', items: SIDOS })
    return fake({ level: 'SIGUNGU', items: sidoCode === '11' ? SEOUL_SIGUNGUS : [] })
  }
  return client.get('/meta/regions', { params: sidoCode ? { sidoCode } : {} })
}

export function saveProfile(payload) {
  if (isFixtureMode()) return fake(() => buildProfileResult(payload), 400)
  return client.post('/profile', payload)
}
