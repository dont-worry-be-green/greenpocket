<script setup>
/*
 * 요금별 평가 결과 — WF-10 (B-5-02)
 *
 * `rows` 는 `result.utilityResults[]` 그대로다.
 *
 * ⚠️ **달성 판정을 합산 목표로 하지 않는다.** 각 행의 `targetRate` 는 그 요금에 걸었던
 * 구간의 하한이라 합산 목표와 다르다 — 도시가스는 12% 를 줄이고도 15% 구간을 걸었으면
 * 미달이다. 서버가 판정한 `achieved` 를 그대로 쓴다.
 *
 * ⚠️ **미달에 빨간 X 를 쓰지 않는다**(api-spec.md 11.1). 걸었던 목표를 적어 두는 것으로 끝낸다.
 *
 * ⚠️ `utilityResults[]` 에는 `displayPrecision` 이 없다(preview.utilities[] 에만 있다).
 * 넘기지 않으면 `formatUsage` 기본값 0 이라 수도 62.7㎥ 가 63㎥ 로 잘린다 → `usagePrecision`.
 */
import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import UtilityIcon from '@/components/eco/UtilityIcon.vue'
import {
  formatPercent,
  formatUnit,
  formatUsage,
  formatUtilityType,
  usagePrecision,
} from '@/utils/format'

defineProps({
  rows: { type: Array, default: () => [] },
})

const usage = (row, key) =>
  formatUsage(row[key], usagePrecision(row.usageUnit), formatUnit(row.usageUnit))
</script>

<template>
  <GpCard title="요금별로 보면" caption="걸어 둔 목표는 요금마다 달라요">
    <div class="border-divider divide-divider divide-y border-t">
      <div v-for="row in rows" :key="row.utilityType" class="py-3">
        <div class="flex items-center gap-3">
          <UtilityIcon :utility-type="row.utilityType" small />
          <span class="text-list-title min-w-0 flex-1">
            {{ formatUtilityType(row.utilityType) }}
          </span>
          <GpDelta :value="row.finalRate" size="sm" :show-word="false" class="flex-none" />
        </div>

        <div class="bg-surface-sub mt-2 rounded-md p-3">
          <div class="flex items-baseline justify-between gap-2">
            <span class="text-caption text-muted">기준</span>
            <span class="text-body">{{ usage(row, 'baselineUsage') }}</span>
          </div>
          <div class="mt-1 flex items-baseline justify-between gap-2">
            <span class="text-caption text-muted">평가 기간</span>
            <span class="text-body">{{ usage(row, 'actualUsage') }}</span>
          </div>
        </div>

        <p class="mt-2 mb-0 flex items-center gap-2">
          <GpTag :tone="row.achieved ? 'positive' : 'sub'" small>
            {{ row.achieved ? '목표 달성' : '목표 미달' }}
          </GpTag>
          <span class="text-caption text-muted">
            걸어 둔 목표는 {{ formatPercent(row.targetRate) }} 줄이기였어요
          </span>
        </p>
      </div>
    </div>
  </GpCard>
</template>
