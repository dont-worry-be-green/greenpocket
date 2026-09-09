/*
 * 미션 카드의 요금별 합계 (B-3-04 · 결정 C-42)
 * 같은 deviceGroup 겹침은 미리보기 `counted:false` 만 믿고 뺀다 — 프론트가 다시 판정하지 않는다.
 */
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import EcoMissionPicker from '@/components/eco/EcoMissionPicker.vue'

const segment = {
  utilityType: 'ELECTRICITY',
  registered: true,
  missionRateCap: 30,
  missions: [
    { missionId: 1, title: '냉방 26℃', difficulty: 'EASY', deviceGroup: '냉방', computedRate: 3 },
    { missionId: 2, title: '에어컨 1시간', difficulty: 'EASY', deviceGroup: '냉방', computedRate: 18 },
    { missionId: 3, title: '대기전력', difficulty: 'EASY', deviceGroup: '대기전력', computedRate: 5 },
  ],
}

function mountPicker(props) {
  return mount(EcoMissionPicker, { props: { segment, selectedIds: [], ...props } })
}

describe('EcoMissionPicker 요금별 합계', () => {
  it('고른 미션이 없으면 안내만 보인다', () => {
    const wrapper = mountPicker()
    expect(wrapper.text()).toContain('전기 고른 미션 합계')
    expect(wrapper.text()).toContain('아직 고른 미션이 없어요')
  })

  it('미리보기가 겹침으로 뺀 미션은 합계에 더하지 않는다', () => {
    const wrapper = mountPicker({
      selectedIds: [1, 2, 3],
      previewItems: [
        { missionId: 1, computedRate: 3, counted: false, exclusionReason: '냉방 겹침 · 합계 제외' },
        { missionId: 2, computedRate: 18, counted: true, exclusionReason: null },
        { missionId: 3, computedRate: 5, counted: true, exclusionReason: null },
      ],
      targetRate: 10,
    })
    expect(wrapper.text()).toContain('3개')
    expect(wrapper.text()).toContain('23%')
    expect(wrapper.text()).toContain('전기 목표 10%를 채웠어요')
  })

  it('목표에 못 미치면 모자란 %p 를 알린다', () => {
    const wrapper = mountPicker({ selectedIds: [3], targetRate: 10 })
    expect(wrapper.text()).toContain('5%')
    expect(wrapper.text()).toContain('전기 목표 10%까지 5%p 모자라요')
  })

  it('구간을 안 골랐으면 합계만 보인다', () => {
    const wrapper = mountPicker({ selectedIds: [3] })
    expect(wrapper.text()).toContain('5%')
    expect(wrapper.text()).not.toContain('목표')
  })

  it('미등록 요금은 합계를 내지 않는다', () => {
    const wrapper = mountPicker({ segment: { ...segment, registered: false }, selectedIds: [3] })
    expect(wrapper.text()).toContain('합계를 내지 않아요')
    expect(wrapper.text()).not.toContain('고른 미션 합계')
  })
})
