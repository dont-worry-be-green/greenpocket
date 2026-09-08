// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import client from '@/api/client'
import { DATA_SOURCE, setDataSource } from '@/api/dataSource'
import {
  getPolicies,
  getPolicy,
  getPolicyRecommendations,
  previewPolicyRecommendations,
} from '@/api/policy'

const storage = new Map()
Object.defineProperty(globalThis, 'localStorage', {
  configurable: true,
  value: {
    clear: () => storage.clear(),
    getItem: (key) => storage.get(key) ?? null,
    removeItem: (key) => storage.delete(key),
    setItem: (key, value) => storage.set(key, String(value)),
  },
})

beforeEach(() => {
  localStorage.clear()
  vi.restoreAllMocks()
})

afterEach(() => setDataSource(DATA_SOURCE.API))

describe('청년정책 API', () => {
  it('맞춤 추천·전체 목록·상세 API 계약을 그대로 호출한다', async () => {
    setDataSource(DATA_SOURCE.API)
    vi.spyOn(client, 'get').mockResolvedValue({})

    await getPolicyRecommendations({ page: 0, size: 5 })
    await getPolicies({ keyword: '주거', page: 0, size: 20 })
    await getPolicy('policy/with space')

    expect(client.get).toHaveBeenNthCalledWith(1, '/policies/recommendations', {
      params: { page: 0, size: 5 },
    })
    expect(client.get).toHaveBeenNthCalledWith(2, '/policies', {
      params: { keyword: '주거', page: 0, size: 20 },
    })
    expect(client.get).toHaveBeenNthCalledWith(3, '/policies/policy%2Fwith%20space')
  })

  it('임시 조건 추천은 저장 API가 아니라 preview API를 호출한다', async () => {
    setDataSource(DATA_SOURCE.API)
    const payload = {
      currentStatus: 'EMPLOYED',
      annualIncomeBand: 'FROM_24M_TO_36M',
      householdStatus: 'ONE_PERSON',
      page: 0,
      size: 5,
    }
    vi.spyOn(client, 'post').mockResolvedValue({ preview: true })

    await previewPolicyRecommendations(payload)

    expect(client.post).toHaveBeenCalledWith('/policies/recommendations/preview', payload)
  })

  it('픽스처 모드에서도 필터·상세·404 흐름을 확인할 수 있다', async () => {
    setDataSource(DATA_SOURCE.FIXTURE)

    const list = await getPolicies({ category: 'HOUSING', page: 0, size: 20 })
    expect(list.content).toHaveLength(1)
    expect(list.content[0].category).toBe('HOUSING')

    const detail = await getPolicy(list.content[0].policyId)
    expect(detail.application.url).toMatch(/^https:/)

    await expect(getPolicy('missing-policy')).rejects.toMatchObject({
      code: 'YOUTH_POLICY_NOT_FOUND',
      status: 404,
    })
  })
})
