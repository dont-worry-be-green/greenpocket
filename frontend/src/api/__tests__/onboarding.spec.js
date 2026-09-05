/*
 * 온보딩 shim 이 **서버 없이도 화면을 끝까지 걷게 하는지** 본다.
 *
 * `startUser` 만 픽스처 모드에서도 실제로 `POST /users` 를 쏜다. 이 테스트 환경에는 서버가 없어
 * 그 호출이 반드시 실패하는데, **실패를 삼키고 픽스처로 넘어가는 것**이 여기의 회귀 테스트다.
 * 이 경로가 깨지면 서버가 꺼진 자리에서 온보딩 첫 화면부터 막힌다.
 */

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'

import { describe, expect, it } from 'vitest'

import * as onboardingApi from '../onboarding'

describe('api/onboarding.js — 엔드포인트 함수', () => {
  it('api-spec.md 4·5절에서 ONB 두 화면이 쓰는 3개만 있다', () => {
    expect(Object.keys(onboardingApi).sort()).toEqual(['getRegions', 'saveProfile', 'startUser'])
  })

  it('연동 스위치는 한 곳에만 있다', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/api/onboarding.js'), 'utf8')
    expect(source.match(/const USE_FIXTURES/g)).toHaveLength(1)
  })
})

describe('startUser — 서버가 없어도 다음 화면으로 넘어간다', () => {
  it('실호출이 실패해도 픽스처로 사용자 정보를 돌려준다', async () => {
    const user = await onboardingApi.startUser({ name: '  김수현  ' })
    expect(user.name).toBe('김수현')
    expect(user.onboardingCompleted).toBe(false)
    expect(user.nextScreen).toBe('ONB-02')
    // 결정 C-14 — 가입 시 서버가 발급하는 형식
    expect(user.pocketAccountNo).toMatch(/^1005-\d{4}-\d{4}-\d{2}$/)
    expect(user.pocketHolder).toBe('김수현')
  })

  it.each([
    ['빈 문자열', ''],
    ['공백만', '   '],
    ['21자', '가'.repeat(21)],
    ['특수문자만', '!!!'],
  ])('%s 은 NAME_INVALID 로 막는다', async (_label, name) => {
    await expect(onboardingApi.startUser({ name })).rejects.toMatchObject({
      code: 'NAME_INVALID',
      status: 400,
    })
  })
})

describe('getRegions — 시도·시군구', () => {
  it('sidoCode 가 없으면 시도 17개를 준다', async () => {
    const regions = await onboardingApi.getRegions()
    expect(regions.level).toBe('SIDO')
    expect(regions.items).toHaveLength(17)
    expect(regions.items[0]).toEqual({ code: '11', name: '서울특별시' })
  })

  it('서울은 자치구 25개이고 지역 평균이 있는 곳은 관악구뿐이다', async () => {
    const regions = await onboardingApi.getRegions({ sidoCode: '11' })
    expect(regions.level).toBe('SIGUNGU')
    expect(regions.items).toHaveLength(25)
    expect(regions.items.filter((item) => item.hasRegionAverage).map((item) => item.name)).toEqual([
      '관악구',
    ])
  })

  // 없는 목록을 지어내지 않는다. 빈 배열은 에러가 아니라 안내다(핵심 규칙 8)
  it('서울 밖 시도는 빈 목록이다 — 에러가 아니다', async () => {
    const regions = await onboardingApi.getRegions({ sidoCode: '26' })
    expect(regions.level).toBe('SIGUNGU')
    expect(regions.items).toEqual([])
  })
})

describe('saveProfile — 고른 값이 요약에 그대로 반영된다', () => {
  it('서울이면 seoulResident 가 true 이고 홈으로 보낸다', async () => {
    const saved = await onboardingApi.saveProfile({
      sidoCode: '11',
      sidoName: '서울특별시',
      sigunguCode: '11620',
      sigunguName: '관악구',
      housingType: 'APARTMENT',
      areaBand: 'OVER_20',
    })
    expect(saved.onboardingCompleted).toBe(true)
    expect(saved.seoulResident).toBe(true)
    expect(saved.profileSummary).toBe('서울 관악구 · 아파트 20평 이상')
    expect(saved.nextScreen).toBe('WF-06')
  })

  it('서울 밖이면 seoulResident 가 false 다 — B-1-09 연동 가능 판정의 근거다', async () => {
    const saved = await onboardingApi.saveProfile({
      sidoCode: '41',
      sidoName: '경기도',
      sigunguCode: '41135',
      sigunguName: '성남시 분당구',
      housingType: 'ONE_ROOM',
      areaBand: 'UNDER_10',
    })
    expect(saved.seoulResident).toBe(false)
    expect(saved.profileSummary).toBe('경기 성남시 분당구 · 원룸 10평 이하')
  })
})
