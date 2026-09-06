/*
 * 온보딩 API 계층 — **데이터 소스 두 갈래를 모두 본다.**
 *
 * 기본값은 실 API 다(`api/dataSource.js`). 이 파일은 모드를 명시적으로 세우고 확인한다.
 *   실 API 모드   → 네트워크로 나간다. 서버가 없으면 실패가 그대로 화면까지 올라온다
 *   목데이터 모드 → `src/fixtures/` 로 화면을 끝까지 걷는다
 *
 * `startUser` 만 목데이터 모드에서도 실제로 `POST /users` 를 쏜다. 이 테스트 환경에는 서버가 없어
 * 그 호출이 반드시 실패하는데, **실패를 삼키고 픽스처로 넘어가는 것**이 여기의 회귀 테스트다.
 * 데모 키가 서버에 등록되지 않으면 목데이터 모드로 온보딩을 걸어도 혜택·포켓 탭이 전부 401 이다.
 */

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'

import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import { DATA_SOURCE, setDataSource } from '../dataSource'
import * as onboardingApi from '../onboarding'

// 기본값(실 API)으로 되돌린다. 저장소가 세션을 넘어 남으면 다른 파일이 목데이터를 보게 된다
afterEach(() => setDataSource(DATA_SOURCE.API))

describe('api/onboarding.js — 엔드포인트 함수', () => {
  const source = readFileSync(resolve(process.cwd(), 'src/api/onboarding.js'), 'utf8')

  it('api-spec.md 4·5절에서 ONB 두 화면이 쓰는 3개만 있다', () => {
    expect(Object.keys(onboardingApi).sort()).toEqual(['getRegions', 'saveProfile', 'startUser'])
  })

  /*
   * 스위치를 이 파일 안에 다시 두면 데모 도구가 바꾸는 값과 갈라진다.
   * 판정은 `api/dataSource.js` 하나가 하고 여기는 물어보기만 한다.
   */
  it('연동 스위치를 자체적으로 들고 있지 않다', () => {
    expect(source).not.toMatch(/const\s+USE_FIXTURES/)
    expect(source).toMatch(/from '\.\/dataSource'/)
  })
})

describe('실 API 모드 — 네트워크로 나간다', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.API))

  /*
   * 서버가 없는 환경이라 반드시 실패한다. **실패한다는 것이 확인 대상이다** —
   * 픽스처로 조용히 넘어가면 연동이 됐는지 아닌지 화면에서 구별할 수 없다.
   */
  it.each([
    ['startUser', () => onboardingApi.startUser({ name: '김수현' })],
    ['getRegions', () => onboardingApi.getRegions()],
    ['saveProfile', () => onboardingApi.saveProfile({ sidoCode: '11' })],
  ])('%s 는 픽스처로 대체되지 않는다', async (_label, call) => {
    await expect(call()).rejects.toMatchObject({ code: 'NETWORK_ERROR' })
  })
})

describe('목데이터 모드 · startUser — 서버가 없어도 다음 화면으로 넘어간다', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))

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

describe('목데이터 모드 · getRegions — 시도·시군구', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))

  /*
   * 서비스 지역이 서울 하나다(결정 C-15 · api-spec.md 4.3). 서버가 1건만 주므로
   * 픽스처도 1건이다 — 목데이터 모드에서만 고를 수 있는 지역을 만들지 않는다.
   */
  it('sidoCode 가 없으면 서울특별시 1건이다', async () => {
    const regions = await onboardingApi.getRegions()
    expect(regions.level).toBe('SIDO')
    expect(regions.items).toEqual([
      { code: '11', name: '서울특별시', sidoCode: '11', hasRegionAverage: true },
    ])
  })

  it('서울은 자치구 25개이고 지역 평균이 있는 곳은 관악구뿐이다', async () => {
    const regions = await onboardingApi.getRegions({ sidoCode: '11' })
    expect(regions.level).toBe('SIGUNGU')
    expect(regions.items).toHaveLength(25)
    expect(regions.items.filter((item) => item.hasRegionAverage).map((item) => item.name)).toEqual([
      '관악구',
    ])
  })

  // 화면에서는 서울 밖을 고를 수 없다. 지어낸 목록을 흘리지 않는지만 확인한다(핵심 규칙 8)
  it('서울 밖 시도는 빈 목록이다 — 에러가 아니다', async () => {
    const regions = await onboardingApi.getRegions({ sidoCode: '26' })
    expect(regions.level).toBe('SIGUNGU')
    expect(regions.items).toEqual([])
  })
})

describe('목데이터 모드 · saveProfile — 고른 값이 요약에 그대로 반영된다', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))

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
