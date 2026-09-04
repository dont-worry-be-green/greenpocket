<script setup>
/*
 * WF-07 어디서 발목을 잡았나 (B-4-07)
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
 */
import { computed, ref, watch } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import UtilityIcon from '@/components/eco/UtilityIcon.vue'
import { formatPercent, formatUnit, formatUsage, formatUtilityType, usagePrecision } from '@/utils/format'

const props = defineProps({
  cause: { type: Object, required: true },
})

const rows = computed(() => props.cause.byUtility ?? [])

/** 손으로 여닫은 것만 담는다. 비어 있으면 서버의 `expanded` 를 따른다 */
const toggled = ref({})
watch(rows, () => (toggled.value = {}))

const isOpen = (row) => toggled.value[row.utilityType] ?? row.expanded
const toggle = (row) => (toggled.value = { ...toggled.value, [row.utilityType]: !isOpen(row) })

const headline = computed(() => {
  const largest = rows.value.find((row) => row.utilityType === props.cause.largestCarbonUtility)
  if (!largest) return ''
  return `우리 집 온실가스의 ${formatPercent(largest.carbonSharePercent)}가 ${formatUtilityType(largest.utilityType)}예요`
})

const usage = (row, key) => formatUsage(row[key], usagePrecision(row.usageUnit), formatUnit(row.usageUnit))
</script>

<template>
  <GpCard title="어디서 발목을 잡았나" :caption="headline">
    <div class="border-divider divide-divider divide-y border-t">
      <div v-for="row in rows" :key="row.utilityType">
        <button
          type="button"
          class="flex w-full cursor-pointer items-center gap-3 border-0 bg-transparent px-0 py-3 text-left"
          :aria-expanded="isOpen(row)"
          @click="toggle(row)"
        >
          <UtilityIcon :utility-type="row.utilityType" small />
          <span class="text-list-title min-w-0 flex-1">
            {{ formatUtilityType(row.utilityType) }}
            <span class="text-caption text-muted font-normal">
              · 온실가스 {{ formatPercent(row.carbonSharePercent) }}
            </span>
          </span>
          <GpDelta :value="row.rate" size="sm" :show-word="false" class="flex-none" />
          <IconChevronRight
            :size="16"
            class="text-muted ease-standard flex-none transition-transform duration-140"
            :class="isOpen(row) ? 'rotate-90' : ''"
          />
        </button>

        <div v-if="isOpen(row)" class="pb-3">
          <div class="bg-surface-sub rounded-md p-3">
            <div class="flex items-baseline justify-between gap-2">
              <span class="text-caption text-muted">기준</span>
              <span class="text-body">{{ usage(row, 'baselineUsage') }}</span>
            </div>
            <div class="mt-1 flex items-baseline justify-between gap-2">
              <span class="text-caption text-muted">이 달</span>
              <span class="text-body">{{ usage(row, 'actualUsage') }}</span>
            </div>
          </div>
          <!-- 달성/미달은 그 요금에 건 목표로 서버가 판정한 값이다. 화면이 다시 재지 않는다 -->
          <p class="mt-2 mb-0">
            <GpTag :tone="row.achieved ? 'positive' : 'negative'" small>
              {{ row.achieved ? '목표 달성' : '목표 미달' }}
            </GpTag>
          </p>
        </div>
      </div>
    </div>

    <!-- 판정 근거를 숨기지 않는다 (핵심 규칙 7). 탄소 환산계수는 서버가 준 값 그대로다 -->
    <p v-if="cause.carbonFactors?.length" class="text-caption text-muted mt-3 mb-0">
      온실가스 환산은
      <template v-for="(factor, index) in cause.carbonFactors" :key="factor.utilityType">
        <template v-if="index > 0"> · </template>
        {{ formatUtilityType(factor.utilityType) }} {{ factor.factorG }}g/{{
          formatUnit(factor.unit)
        }}
      </template>
      기준이에요.
    </p>
  </GpCard>
</template>
