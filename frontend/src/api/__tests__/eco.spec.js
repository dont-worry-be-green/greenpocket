/*
 * 픽스처 격리 가드.
 *
 * 화면이 `src/fixtures/` 를 직접 import 하기 시작하면 연동 시점에 뷰를 전부 고쳐야 한다.
 * **픽스처를 아는 파일은 `src/api/eco.js` 하나여야 한다** — 거기 `USE_FIXTURES` 한 줄만 뒤집으면
 * 스토어와 뷰는 그대로 산다.
 *
 * 이 테스트가 깨지면 import 를 지우는 게 정답이지, 목록에 파일을 더하는 게 아니다.
 */

import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative, resolve, sep } from 'node:path'
import process from 'node:process'

import { describe, expect, it } from 'vitest'

import * as ecoApi from '../eco'

// jsdom 환경에서는 import.meta.url 이 file: 스킴이 아니다. vitest 의 root 가 frontend/ 다
const SRC = resolve(process.cwd(), 'src')
const SOURCE_EXTENSIONS = ['.js', '.vue']

function sourceFiles(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name)
    if (statSync(path).isDirectory()) return sourceFiles(path)
    return SOURCE_EXTENSIONS.some((ext) => name.endsWith(ext)) ? [path] : []
  })
}

const importsFixtures = (path) => /from\s+['"]@\/fixtures/.test(readFileSync(path, 'utf8'))

const toPosix = (path) => relative(SRC, path).split(sep).join('/')

/*
 * 아직 남아 있는 위반. **배치 2(WF-06)에서 WhatIfHomeView 가 스토어 구동으로 바뀌면 사라진다.**
 * 그때 이 목록을 비우면 테스트가 다시 통과한다.
 */
const KNOWN_VIOLATIONS = ['views/eco/WhatIfHomeView.vue']

describe('픽스처 격리', () => {
  const violations = sourceFiles(SRC)
    .filter(importsFixtures)
    .map(toPosix)
    .filter((path) => path !== 'api/eco.js')
    .sort()

  it('api/eco.js 말고는 @/fixtures 를 import 하지 않는다', () => {
    expect(violations).toEqual(KNOWN_VIOLATIONS)
  })

  it('api/eco.js 는 픽스처를 알고 있다 — 여기가 유일한 창구다', () => {
    expect(importsFixtures(join(SRC, 'api/eco.js'))).toBe(true)
  })

  it('연동 스위치는 한 곳에만 있다', () => {
    const source = readFileSync(join(SRC, 'api/eco.js'), 'utf8')
    expect(source.match(/const USE_FIXTURES/g)).toHaveLength(1)
  })
})

describe('api/eco.js — 엔드포인트 함수', () => {
  const EXPECTED = [
    'applyRound',
    'createGoal',
    'getCurrentRound',
    'getEcoHome',
    'getEcoLinkJob',
    'getEcoStatus',
    'getGoal',
    'getGoalForm',
    'getMissionAdjust',
    'getMonthlyReport',
    'getRoundResult',
    'getSettlement',
    'getTodayMissions',
    'linkEco',
    'markResultViewed',
    'previewGoal',
    'saveMissionLog',
    'updateGoal',
    'updateMissions',
  ]

  it('api-spec.md 8~11절의 19개가 모두 있다', () => {
    expect(Object.keys(ecoApi).sort()).toEqual(EXPECTED)
  })
})

describe('픽스처 shim 동작', () => {
  it('goal-form 은 세 요금 세그먼트를 준다', async () => {
    const goalForm = await ecoApi.getGoalForm(7)
    expect(goalForm.segments.map((segment) => segment.utilityType)).toEqual([
      'ELECTRICITY',
      'GAS',
      'WATER',
    ])
  })

  it('preview 는 구간에 따라 값이 달라진다 — 상수가 아니다', async () => {
    const low = await ecoApi.previewGoal(7, {
      targets: [{ utilityType: 'ELECTRICITY', tier: 'TIER_5' }],
      selectedMissionIds: [],
    })
    const high = await ecoApi.previewGoal(7, {
      targets: [{ utilityType: 'ELECTRICITY', tier: 'TIER_15' }],
      selectedMissionIds: [],
    })
    expect(high.combined.combinedRate).toBeGreaterThan(low.combined.combinedRate)
  })

  it('저장하면 다시 연 goal-form 에 고른 구간과 미션이 남는다', async () => {
    await ecoApi.createGoal(7, {
      targets: [{ utilityType: 'ELECTRICITY', tier: 'TIER_15' }],
      selectedMissionIds: [13],
    })
    const goalForm = await ecoApi.getGoalForm(7)
    const electricity = goalForm.segments[0]
    expect(electricity.selectedTier).toBe('TIER_15')
    expect(electricity.missions.find((mission) => mission.missionId === 13).selected).toBe(true)
    expect(electricity.missions.find((mission) => mission.missionId === 12).selected).toBe(false)
  })

  it('저장 응답의 숫자는 preview 와 같은 계산에서 나온다', async () => {
    const request = {
      targets: [
        { utilityType: 'ELECTRICITY', tier: 'TIER_10' },
        { utilityType: 'GAS', tier: 'TIER_15' },
        { utilityType: 'WATER', tier: 'TIER_5' },
      ],
      selectedMissionIds: [13],
    }
    const preview = await ecoApi.previewGoal(7, request)
    const saved = await ecoApi.updateGoal(7, request)
    expect(saved.combinedTargetRate).toBe(preview.combined.combinedRate)
    expect(saved.expectedMileage).toBe(preview.combined.expectedMileage)
    expect(saved.expectedSavingAmount).toBe(preview.combined.totalExpectedSaving)
    expect(saved.nextScreen).toBe('WF-06')
  })

  /*
   * `?preview=WF-05` 판정이 뷰가 아니라 shim 에 있다는 것을 못 박는다.
   * 뷰가 쿼리로 데이터를 갈아끼우면 연동 후 지울 곳이 화면 코드에 흩어진다.
   */
  it('?preview=WF-05 면 수도가 미등록으로 오고 preview 도 같은 판을 쓴다', async () => {
    window.history.replaceState({}, '', '/whatif/goal?preview=WF-05')
    try {
      const goalForm = await ecoApi.getGoalForm(7)
      expect(goalForm.segments[2].registered).toBe(false)

      const preview = await ecoApi.previewGoal(7, {
        targets: [{ utilityType: 'ELECTRICITY', tier: 'TIER_10' }],
        selectedMissionIds: [],
      })
      expect(preview.combined.excludedUtilities).toEqual(['WATER'])
    } finally {
      window.history.replaceState({}, '', '/')
    }
  })

  it('연동 폴링은 호출할 때마다 한 단계씩 나아가고 마지막에 SUCCEEDED 가 된다', async () => {
    await ecoApi.linkEco()
    const statuses = []
    for (let i = 0; i < 4; i += 1) {
      const job = await ecoApi.getEcoLinkJob('demo-link-job')
      statuses.push(job.status)
    }
    expect(statuses).toEqual(['RUNNING', 'RUNNING', 'RUNNING', 'SUCCEEDED'])
  })
})
