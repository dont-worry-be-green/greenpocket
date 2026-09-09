import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SingleHouseholdComparisonCard from '@/components/analysis/SingleHouseholdComparisonCard.vue'

const comparison = {
  tabs: [
    {
      utilityType: 'ELECTRICITY',
      available: true,
      myUsage: 210,
      averageUsage: 257.617,
      differenceUsage: -47.617,
      differenceRate: -18.483,
      usageUnit: 'kWh',
      comparisonLabel: '전국 1인 가구',
      sourceName: '에너지경제연구원 2023년 기준 14차 가구에너지패널조사 마이크로데이터',
      referencePeriod: '2023년 기준',
      calculationBasis: '1인 가구 1,221가구의 가구 횡단가중 평균',
      note: '전국 표본 조사 추정치예요.',
      series: [
        { yearMonth: '2026-03', myUsage: 195, averageUsage: 189.658 },
        { yearMonth: '2026-04', myUsage: null, averageUsage: 184.783 },
        { yearMonth: '2026-05', myUsage: 188, averageUsage: 179.013 },
        { yearMonth: '2026-06', myUsage: 190, averageUsage: 186.26 },
        { yearMonth: '2026-07', myUsage: 205, averageUsage: 228.449 },
        { yearMonth: '2026-08', myUsage: 210, averageUsage: 257.617 },
      ],
    },
    {
      utilityType: 'GAS',
      available: false,
      unavailableReason: 'BASELINE_NOT_AVAILABLE',
      series: [],
    },
  ],
}

describe('SingleHouseholdComparisonCard', () => {
  it('최근 6개월의 나와 1인 가구 평균 시계열을 표시한다', () => {
    const wrapper = mount(SingleHouseholdComparisonCard, { props: { comparison } })

    expect(wrapper.text()).toContain('1인 가구 평균 사용량')
    expect(wrapper.text()).toContain('210.0kWh')
    expect(wrapper.text()).toContain('257.6kWh')
    expect(wrapper.text()).toContain('평균보다 47.6kWh (18.5%) 더 적게 사용했어요')
    expect(wrapper.text()).toContain('3월')
    expect(wrapper.text()).toContain('8월')
    expect(wrapper.find('[data-testid="average-line"]').attributes('d')).toContain('L')
    expect(wrapper.find('svg').attributes('viewBox')).toBe('0 0 300 100')
    expect(wrapper.text()).toContain('출처 · 에너지경제연구원')
    expect(wrapper.text()).not.toContain('2023년 기준')
  })

  it('월 축에 마우스를 올리면 해당 월의 나와 평균 사용량을 표시한다', async () => {
    const wrapper = mount(SingleHouseholdComparisonCard, { props: { comparison } })

    await wrapper.findAll('[data-testid="month-axis"]')[0].trigger('mouseenter')

    const tooltip = wrapper.get('[data-testid="usage-tooltip"]')
    expect(tooltip.text()).toContain('3월')
    expect(tooltip.text()).toContain('나 195.0kWh')
    expect(tooltip.text()).toContain('1인 가구 평균 189.7kWh')
  })

  it('고지서가 없는 달은 나의 선을 연결하지 않고 점도 그리지 않는다', () => {
    const wrapper = mount(SingleHouseholdComparisonCard, { props: { comparison } })
    const path = wrapper.find('[data-testid="my-line"]').attributes('d')

    expect(path.match(/M/g)).toHaveLength(2)
    expect(wrapper.findAll('[data-testid="my-point"]')).toHaveLength(5)
    expect(wrapper.text()).toContain('고지서가 없는 달은 나의 사용량 선을 연결하지 않았어요.')
  })

  it('기준선이 없는 요금 탭은 정상 빈 상태로 표시한다', async () => {
    const wrapper = mount(SingleHouseholdComparisonCard, { props: { comparison } })

    await wrapper.findAll('[role="tab"]')[1].trigger('click')

    expect(wrapper.text()).toContain('1인 가구 비교 데이터를 준비하고 있어요.')
    expect(wrapper.find('[data-testid="my-line"]').exists()).toBe(false)
  })
})
