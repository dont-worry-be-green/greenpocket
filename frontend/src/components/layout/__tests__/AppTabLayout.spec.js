import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

import AppTabLayout from '../AppTabLayout.vue'
import { TABS } from '../tabs'

import onboarding from '@/router/routes/onboarding'
import analysis from '@/router/routes/analysis'
import greenlife from '@/router/routes/greenlife'
import eco from '@/router/routes/eco'
import pocket from '@/router/routes/pocket'
import mypage from '@/router/routes/mypage'

/*
 * COM-02 완료 조건 — "5개 탭 라벨·순서가 모든 화면에서 동일하다"
 * 라벨·순서·경로는 결정 B-2 로 고정된 것이라 코드로 잠가 둔다.
 */
function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [...onboarding, ...analysis, ...greenlife, ...eco, ...pocket, ...mypage],
  })
}

async function mountAt(tab) {
  const router = makeRouter()
  await router.push('/whatif')
  await router.isReady()
  const wrapper = mount(AppTabLayout, {
    props: { tab, title: '제목' },
    global: { plugins: [router] },
  })
  return { wrapper, router }
}

let router
let wrapper

beforeEach(async () => {
  ;({ wrapper, router } = await mountAt('whatif'))
})

describe('탭 정의', () => {
  it('라벨과 순서가 진단·혜택·What-if·포켓·마이페이지로 고정된다', () => {
    expect(TABS.map((t) => t.label)).toEqual(['진단', '혜택', 'What-if', '포켓', '마이페이지'])
  })

  it('가운데 What-if 하나만 FAB 이다', () => {
    expect(TABS.filter((t) => t.fab).map((t) => t.key)).toEqual(['whatif'])
  })

  it('탭 경로가 모두 라우터에 등록되어 있다', () => {
    const router = makeRouter()
    for (const t of TABS) {
      expect(router.resolve(t.path).matched.length, `${t.path} 라우트 없음`).toBeGreaterThan(0)
    }
  })
})

describe('AppTabLayout', () => {
  it('탭 5개를 라벨 순서대로 렌더한다', () => {
    const labels = wrapper.findAll('[role="tab"]').map((b) => b.text())
    expect(labels).toEqual(['진단', '혜택', 'What-if', '포켓', '마이페이지'])
  })

  it('현재 탭만 aria-selected 다', () => {
    const selected = wrapper
      .findAll('[role="tab"]')
      .filter((b) => b.attributes('aria-selected') === 'true')
    expect(selected).toHaveLength(1)
    expect(selected[0].text()).toBe('What-if')
  })

  it('다른 탭을 누르면 그 탭의 경로로 이동한다', async () => {
    await wrapper.findAll('[role="tab"]')[3].trigger('click')
    /*
     * 탭 화면은 () => import(...) 로 지연 로딩된다. 클릭한 뒤에야 파일을 가지러 가므로
     * 정해진 만큼만 기다리면 아직 이전 경로에 있다. flushPromises 로는 부족하다 —
     * 파일을 가져오는 것은 밀려 있던 작업이 아니라 그때 새로 시작하는 일이라
     * 여러 번 불러도 기다려지지 않는다. 도착할 때까지 다시 확인한다.
     */
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/pocket'))
  })

  it('현재 탭을 다시 눌러도 이동하지 않는다', async () => {
    const before = router.currentRoute.value.fullPath
    await wrapper.findAll('[role="tab"]')[2].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe(before)
  })
})
