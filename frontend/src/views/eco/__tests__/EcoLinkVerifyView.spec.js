/*
 * WF-01a 본인확인 · 연동 — **기능명세서에 없는 화면이다.** 사정은 `api/eco.js` 의
 * `verifyEcoIdentity` 주석에 적어 두었다.
 *
 * 단계가 셋이다 — PHONE(번호·동의) → CODE(인증번호 6자리) → LINK(연동하기).
 *
 * 여기서 지키려는 것은 다섯이다.
 *   1. 동의 없이는 인증번호를 보내지 않는다 (핵심 비즈니스 규칙 4)
 *   2. **틀린 인증번호는 통과하지 않는다** — 아무 여섯 자리나 되면 검증이 없는 것과 같다
 *   3. **인증은 연동을 시작하지 않는다** — 묶이면 사용자가 무엇에 동의해 무엇이 일어났는지
 *      구분할 수 없다
 *   4. 「연동하기」를 눌러야 연동이 걸리고 홈으로 넘어간다 — 폴링은 홈이 받는다
 *   5. 미가입·등록 정보 없음은 화면을 가르지 않고 **하단에 길만 열어 둔다** (핵심 규칙 8)
 *
 * ⚠️ **목데이터 모드를 세우고 돈다.** 기본값은 실 API 라 그대로 두면 서버가 없는 이 환경에서
 * 화면이 에러만 그린다. 발송·확인 모의가 각각 0.7초·0.9초라 실제 타이머로 돌린다 —
 * 여기서 시간을 가짜로 만들면 검사할 것이 사라진다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import { DATA_SOURCE, setDataSource } from '@/api/dataSource'
import routes from '@/router/routes/eco'
import EcoLinkVerifyView from '@/views/eco/EcoLinkVerifyView.vue'

async function mountView() {
  window.history.replaceState({}, '', '/whatif/link')
  const router = createRouter({ history: createWebHistory(), routes })
  await router.push('/whatif/link')
  await router.isReady()

  const wrapper = mount(EcoLinkVerifyView, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  // GET /eco/status 를 기다린다. externalUrl 이 여기서 온다
  await new Promise((resolve) => setTimeout(resolve, 400))
  await flushPromises()

  return { wrapper, router }
}

/** 라벨이 아니라 보이는 글자로 찾는다 — 사용자가 누르는 것과 같은 기준이다 */
function buttonWith(wrapper, text) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

async function fillForm(wrapper, { agree = true } = {}) {
  await wrapper.find('input[type="text"]').setValue('김수현')
  await buttonWith(wrapper, 'SKT').trigger('click')
  await wrapper.find('input[type="tel"]').setValue('01012345678')
  if (agree) await wrapper.find('input[type="checkbox"]').setValue(true)
}

async function settle(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms))
  await flushPromises()
}

/** PHONE → CODE. 발송 모의 0.7초를 실제로 기다린다 */
async function requestCode(wrapper) {
  await fillForm(wrapper)
  await buttonWith(wrapper, '인증번호 받기').trigger('click')
  await settle(1000)
}

/** CODE → LINK. 데모 코드는 `000000` 이고 화면 캡션이 그 값을 밝힌다 */
async function enterCode(wrapper, code = '000000') {
  await wrapper.find('input[inputmode="numeric"]').setValue(code)
  await buttonWith(wrapper, '확인').trigger('click')
  await settle(1200)
}

/** 인증 끝까지 (PHONE → CODE → LINK) */
async function verify(wrapper) {
  await requestCode(wrapper)
  await enterCode(wrapper)
}

describe('EcoLinkVerifyView', () => {
  beforeEach(() => {
    localStorage.clear()
    // clear() 가 데이터 소스 키까지 지운다. 지운 뒤에 세운다
    setDataSource(DATA_SOURCE.FIXTURE)
  })
  afterEach(() => setDataSource(DATA_SOURCE.API))

  it('동의하지 않으면 인증번호를 보낼 수 없다 — 입력을 다 채워도 마찬가지다', async () => {
    const { wrapper } = await mountView()

    await fillForm(wrapper, { agree: false })
    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeDefined()

    await wrapper.find('input[type="checkbox"]').setValue(true)
    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeUndefined()

    wrapper.unmount()
  })

  it('인증번호 화면으로 넘어가면 보낸 번호와 데모 코드를 밝힌다', async () => {
    const { wrapper } = await mountView()

    await requestCode(wrapper)

    expect(wrapper.text()).toContain('인증번호를 입력해 주세요')
    expect(wrapper.text()).toContain('010-1234-5678')
    // 발표자가 맞힐 수 있어야 한다. 실제 엔드포인트가 생기면 이 캡션은 사라진다
    expect(wrapper.text()).toContain('000000')

    wrapper.unmount()
  })

  it('여섯 자리를 다 넣어야 확인이 열린다', async () => {
    const { wrapper } = await mountView()
    await requestCode(wrapper)

    await wrapper.find('input[inputmode="numeric"]').setValue('00000')
    expect(buttonWith(wrapper, '확인').attributes('disabled')).toBeDefined()

    await wrapper.find('input[inputmode="numeric"]').setValue('000000')
    expect(buttonWith(wrapper, '확인').attributes('disabled')).toBeUndefined()

    wrapper.unmount()
  })

  it('틀린 인증번호는 통과하지 않는다 — 아무 여섯 자리나 되면 검증이 없는 것과 같다', async () => {
    const { wrapper } = await mountView()
    await requestCode(wrapper)

    await enterCode(wrapper, '123456')

    expect(wrapper.text()).toContain('인증번호가 맞지 않아요')
    // 다음 단계로 넘어가지 않는다
    expect(wrapper.text()).not.toContain('확인됐어요')

    // 맞는 번호를 넣으면 통과한다
    await enterCode(wrapper, '000000')
    expect(wrapper.text()).toContain('김수현님, 확인됐어요')

    wrapper.unmount()
  })

  it('인증이 끝나도 연동을 시작하지 않는다 — 다음 단계를 보여주고 멈춘다', async () => {
    const { wrapper, router } = await mountView()

    await verify(wrapper)

    // 인증 결과를 보여주되 아직 홈으로 넘기지 않는다
    expect(wrapper.text()).toContain('김수현님, 확인됐어요')
    expect(buttonWith(wrapper, '에코마일리지 연동하기')).toBeDefined()
    expect(router.currentRoute.value.path).toBe('/whatif/link')

    wrapper.unmount()
  })

  it('「연동하기」를 눌러야 연동이 걸리고 홈으로 넘어간다 — 폴링은 홈이 받는다', async () => {
    const { wrapper, router } = await mountView()

    await verify(wrapper)
    await buttonWith(wrapper, '에코마일리지 연동하기').trigger('click')

    await new Promise((resolve) => setTimeout(resolve, 700))
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/whatif')
    wrapper.unmount()
  })

  it('미가입 안내는 화면을 가르지 않고 연동 단계 하단에 상시 있다', async () => {
    const { wrapper } = await mountView()

    // 인증 전에는 폼만 있다
    expect(wrapper.text()).toContain('본인확인이 필요해요')
    expect(wrapper.text()).not.toContain('아직 회원이 아니거나')

    // 인증번호 화면에도 없다 — 아직 판정할 수 있는 단계가 아니다
    await requestCode(wrapper)
    expect(wrapper.text()).not.toContain('아직 회원이 아니거나')

    await enterCode(wrapper)

    // 연동 버튼과 누리집 안내가 같은 화면에 함께 있다
    expect(wrapper.text()).toContain('아직 회원이 아니거나 등록 정보가 없나요?')
    expect(buttonWith(wrapper, '에코마일리지 연동하기')).toBeDefined()

    wrapper.unmount()
  })

  it('가입은 누리집에서 한다 — 주소는 GET /eco/status 가 준다', async () => {
    const { wrapper } = await mountView()
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)

    await verify(wrapper)
    await buttonWith(wrapper, '누리집에서 에코마일리지 시작하기').trigger('click')

    expect(open).toHaveBeenCalledTimes(1)
    expect(open.mock.calls[0][0]).toMatch(/^https?:\/\//)
    expect(open.mock.calls[0][2]).toBe('noopener')

    open.mockRestore()
    wrapper.unmount()
  })
})
