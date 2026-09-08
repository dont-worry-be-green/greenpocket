// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import * as onboardingApi from '@/api/onboarding'
import client from '@/api/client'
import { DATA_SOURCE, setDataSource } from '@/api/dataSource'

beforeEach(() => {
  localStorage.clear()
  vi.restoreAllMocks()
})

afterEach(() => setDataSource(DATA_SOURCE.API))

describe('온보딩 API', () => {
  it('기존 데모 사용자 생성 API를 노출하지 않는다', () => {
    expect(Object.keys(onboardingApi).sort()).toEqual(['getRegions', 'saveProfile'])
  })

  it('실 API 모드에서 지역 목록을 조회한다', async () => {
    setDataSource(DATA_SOURCE.API)
    vi.spyOn(client, 'get').mockResolvedValue({ level: 'SIDO', items: [] })

    await onboardingApi.getRegions({ sidoCode: '11' })

    expect(client.get).toHaveBeenCalledWith('/meta/regions', { params: { sidoCode: '11' } })
  })

  it('실 API 모드에서 인증된 사용자의 프로필을 저장한다', async () => {
    setDataSource(DATA_SOURCE.API)
    const payload = {
      sidoCode: '11',
      sigunguCode: '11620',
      housingType: 'STUDIO',
      areaBand: 'UNDER_10',
    }
    vi.spyOn(client, 'post').mockResolvedValue({ nextScreen: 'WF-06' })

    await onboardingApi.saveProfile(payload)

    expect(client.post).toHaveBeenCalledWith('/profile', payload)
  })

  it('픽스처 시도 목록은 서울특별시 1건이다', async () => {
    setDataSource(DATA_SOURCE.FIXTURE)
    const result = await onboardingApi.getRegions()
    expect(result.level).toBe('SIDO')
    expect(result.items).toHaveLength(1)
    expect(result.items[0]).toMatchObject({ code: '11', name: '서울특별시' })
  })

  it('픽스처 서울 시군구 목록은 25개다', async () => {
    setDataSource(DATA_SOURCE.FIXTURE)
    const result = await onboardingApi.getRegions({ sidoCode: '11' })
    expect(result.level).toBe('SIGUNGU')
    expect(result.items).toHaveLength(25)
  })

  it('서울 밖 시군구는 빈 목록이다', async () => {
    setDataSource(DATA_SOURCE.FIXTURE)
    const result = await onboardingApi.getRegions({ sidoCode: '26' })
    expect(result.items).toEqual([])
  })
})
