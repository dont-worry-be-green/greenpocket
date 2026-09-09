<script setup>
/*
 * WF-07 어디가 발목을 잡았나 (B-4-07 ②)
 *
 * `cause` 는 GET /eco/monthly-report 의 `cause` 그대로다.
 *
 * ⚠️ **펼침 여부는 서버가 정한다.** `byUtility[].expanded` 가 「미달이면 펼치고 달성이면 접는다」를
 * 이미 판정해서 내려온다. 화면이 `rate < 0` 같은 조건으로 다시 판정하면 서버와 어긋난다.
 * 사용자가 손으로 여닫은 것만 이 컴포넌트가 기억한다.
 *
 * ⚠️ `carbonSharePercent` 는 **감축률이 아니다.** 「우리 집 온실가스에서 이 요금이 차지하는 몫」이라
 * GpDelta 에 넘기면 "줄었어요" 가 붙어 문장이 틀린다 → formatPercent 로 그린다.
 *
 * ⚠️ `byUtility[]` 에는 `displayPrecision` 이 없다(preview.utilities[] 에만 있다).
 * 넘기지 않으면 formatUsage 기본값 0 이라 수도 9.8㎥ 가 10㎥ 로 잘린다 → usagePrecision 으로 유도한다.
 *
 * ⚠️ **시안의 계절 해설("7월은 더위로 냉방이 늘었을 수 있어요")은 넣지 않는다.**
 * `cause` 응답에 그 문장도, 그것을 만들 계절 필드도 없다. 화면이 지어내면 8월에도 7월 문장이
 * 나간다(AGENTS 3절 · 핵심 규칙 8). 서버가 문구를 내려주면 그때 이 자리에 붙인다.
 */
import { computed, ref, watch } from 'vue'

import UtilityIcon from '@/components/eco/UtilityIcon.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import {
  formatNumber,
  formatPercent,
  formatUnit,
  formatUsage,
  formatUtilityType,
  usagePrecision,
} from '@/utils/format'

const props = defineProps({
  cause: { type: Object, required: true },
})

const rows = computed(() => props.cause.byUtility ?? [])

/** 손으로 여닫은 것만 담는다. 비어 있으면 서버의 `expanded` 를 따른다 */
const toggled = ref({})
watch(rows, () => (toggled.value = {}))

const isOpen = (row) => toggled.value[row.utilityType] ?? row.expanded
const toggle = (row) => (toggled.value = { ...toggled.value, [row.utilityType]: !isOpen(row) })

const usage = (row, key) => formatUsage(row[key], usagePrecision(row.usageUnit))

/** '265 → 270kWh' — 단위는 뒤에 한 번만 붙인다 */
const usageChange = (row) =>
  `${usage(row, 'baselineUsage')} → ${usage(row, 'actualUsage')}${formatUnit(row.usageUnit)}`

/*
 * 온실가스 비중 해설(B-4-07 ②). 숫자는 서버가 준 `carbonSharePercent` 그대로다.
 * 비중이 가장 큰 요금에만 왜 중요한지를 덧붙인다 — 세 줄 모두에 붙이면 설명이 아니라 잡음이다.
 */
function shareText(row) {
  const base = `우리 집 온실가스의 ${formatPercent(row.carbonSharePercent)}가 ${formatUtilityType(row.utilityType)}예요.`
  return row.utilityType === props.cause.largestCarbonUtility
    ? `${base} 조금만 늘어도 합산이 크게 흔들려요.`
    : base
}
</script>

<template>
  <!-- 제목 옆 「온실가스 환산」 배지는 뺐다(2026-09-10 수현 · C-41). 환산 근거는 행 안 비중 해설이 말한다 -->
  <GpCard title="어디가 발목을 잡았나">
    <div class="border-divider divide-divider divide-y border-t">
      <div v-for="row in rows" :key="row.utilityType">
        <button
          type="button"
          class="flex w-full cursor-pointer items-center gap-3 border-0 bg-transparent px-0 py-3 text-left"
          :aria-expanded="isOpen(row)"
          @click="toggle(row)"
        >
          <UtilityIcon :utility-type="row.utilityType" small />
          <span class="min-w-0 flex-1">
            <span class="text-list-title text-ink block">{{
              formatUtilityType(row.utilityType)
            }}</span>
            <span class="text-caption text-muted tabular-nums">{{ usageChange(row) }}</span>
          </span>

          <span class="flex flex-none flex-col items-end gap-1">
            <GpDelta :value="row.rate" size="sm" :show-word="false" />
            <!-- 달성/미달은 그 요금에 건 목표로 서버가 판정한 값이다. 화면이 다시 재지 않는다 -->
            <GpTag :tone="row.achieved ? 'positive' : 'sub'" small>
              {{ row.achieved ? '목표 달성' : '목표 미달' }}
            </GpTag>
          </span>

          <IconChevronRight
            :size="16"
            class="text-icon-off ease-standard flex-none transition-transform duration-140"
            :class="isOpen(row) ? 'rotate-90' : ''"
          />
        </button>

        <p v-if="isOpen(row)" class="text-caption text-muted mt-0 mb-3">{{ shareText(row) }}</p>
      </div>
    </div>

    <!-- 판정 근거를 숨기지 않는다 (핵심 규칙 7). 탄소 환산계수는 서버가 준 값 그대로다 -->
    <p v-if="cause.carbonFactors?.length" class="text-caption text-muted mt-3 mb-0">
      <template v-for="(factor, index) in cause.carbonFactors" :key="factor.utilityType">
        <template v-if="index > 0"> · </template>
        {{ formatUtilityType(factor.utilityType) }} 1{{ formatUnit(factor.unit) }}={{
          formatNumber(factor.factorG)
        }}g </template
      >CO₂ (에코마일리지)
    </p>
  </GpCard>
</template>
