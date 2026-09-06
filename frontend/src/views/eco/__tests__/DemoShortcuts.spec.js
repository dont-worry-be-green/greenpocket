/*
 * 데모 도구의 화면 바로가기가 **죽은 링크가 되지 않는지** 본다.
 *
 * 바로가기는 정상 흐름으로는 여러 단계를 거쳐야 닿는 상태를 한 번에 여는 통로다.
 * 그래서 그 화면들이 **홈을 거치지 않고 들어와도** 그려져야 한다 — 회차 번호를 스토어에서
 * 가져오는 WF-04·WF-08 이 특히 그렇다(각 뷰가 `fetchCurrentRound()` 로 스스로 채운다).
 * 경로나 쿼리 키가 바뀌면 패널의 버튼이 조용히 빈 화면을 열게 되는데, 여기서 걸린다.
 *
 * ⚠️ **목데이터 모드를 세우고 돈다.** 기본값은 실 API 라 그대로 두면 서버가 없는 이 환경에서
 * 전부 에러 화면이 되어 검사할 것이 사라진다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory, RouterView } from 'vue-router'

import { DATA_SOURCE, setDataSource } from '@/api/dataSource'
import { DEMO_SHORTCUTS } from '@/components/layout/demoShortcuts'
import routes from '@/router/routes/eco'

/*
 * eco 라우트만 등록해서 나는 링크 경고(/mypage · /pocket)와, 픽스처 모드에서도 실제로 나가는
 * 네트워크 실패를 거른다. 화면이 낸 오류와 섞이면 판정이 무의미해진다.
 */
const HARNESS_NOISE = /VUE_ROUTER_R0004|ECONNREFUSED|Cross origin|Error: connect|Network Error/

/** 픽스처 지연(120~1500ms)이 끝날 때까지 기다린다 */
async function settle() {
  await flushPromises()
  await new Promise((resolve) => setTimeout(resolve, 600))
  await flushPromises()
}

async function openPath(path) {
  window.history.replaceState({}, '', path)
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({ history: createWebHistory(), routes })
  await router.push(path)
  await router.isReady()

  const errors = []
  const collect = (...args) => errors.push(String(args[0]))
  const errorSpy = vi.spyOn(console, 'error').mockImplementation(collect)
  const warnSpy = vi.spyOn(console, 'warn').mockImplementation(collect)

  // 뷰를 직접 지목하지 않는다 — 라우터가 고르게 두어야 경로 오타까지 잡힌다
  const wrapper = mount(RouterView, { global: { plugins: [pinia, router] } })
  await settle()

  errorSpy.mockRestore()
  warnSpy.mockRestore()
  return { wrapper, errors: errors.filter((message) => !HARNESS_NOISE.test(message)) }
}

const ALL = DEMO_SHORTCUTS.flatMap((section) =>
  section.items.map((item) => [item.label, item.to, section.group]),
)

describe('데모 도구 · 화면 바로가기', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))
  afterEach(() => setDataSource(DATA_SOURCE.API))

  it('빈 그룹이나 중복 경로가 없다', () => {
    expect(ALL.length).toBeGreaterThan(0)
    const paths = ALL.map(([, to]) => to)
    expect(new Set(paths).size).toBe(paths.length)
  })

  it.each(ALL)('%s (%s) 가 열린다', async (_label, to) => {
    const { wrapper, errors } = await openPath(to)

    // 라우터가 그 경로를 못 찾으면 아무것도 그려지지 않는다
    expect(wrapper.html()).not.toBe('<!--v-if-->')
    expect(errors).toEqual([])
    wrapper.unmount()
  })
})
