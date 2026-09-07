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
 * 그래서 배지 문구가 달성/미달로 갈린다 — 달성은 「달성」 한 마디, 미달은 **걸었던 목표**를 적는다.
 * "미달" 이라는 말보다 "목표 15% 줄이기" 가 다음에 뭘 하면 되는지를 알려준다.
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
  reportMode: { type: Boolean, default: false },
})

const usage = (row, key) => formatUsage(row[key], usagePrecision(row.usageUnit))

/** '1,340 → 1,166kWh' — 단위는 뒤에 한 번만 붙인다 */
const usageChange = (row) =>
  `${usage(row, 'baselineUsage')} → ${usage(row, 'actualUsage')}${formatUnit(row.usageUnit)}`

const progressWidth = (rate) => `${Math.min(100, Math.max(0, Number(rate) / 30 * 100))}%`
</script>

<template>
  <GpCard :title="reportMode ? '항목 별로 보면' : '요금별로 보면'">
    <div class="border-divider divide-divider divide-y border-t">
      <div v-for="row in rows" :key="row.utilityType" :class="reportMode ? 'py-4' : 'py-3'">
        <div class="flex items-center gap-3">
          <UtilityIcon :utility-type="row.utilityType" small />

          <span class="min-w-0 flex-1">
            <span class="text-list-title text-ink block">{{
              formatUtilityType(row.utilityType)
            }}</span>
            <span class="text-caption text-muted tabular-nums">{{ usageChange(row) }}</span>
          </span>

          <span class="flex flex-none flex-col items-end gap-1">
            <GpDelta :value="row.finalRate" size="sm" :show-word="false" />
            <GpTag v-if="!reportMode" :tone="row.achieved ? 'positive' : 'sub'" small>
              {{ row.achieved ? '달성' : `목표 ${formatPercent(row.targetRate)} 줄이기` }}
            </GpTag>
          </span>
        </div>

        <div
          v-if="reportMode"
          class="bg-surface-sub mt-3 h-1.5 overflow-hidden rounded-full"
          aria-hidden="true"
        >
          <div
            class="bg-primary-soft h-full rounded-full transition-[width]"
            :style="{ width: progressWidth(row.finalRate) }"
          />
        </div>
      </div>
    </div>

    <p v-if="!reportMode" class="text-caption text-muted mt-3 mb-0">
      목표에 못 미쳐도 줄인 만큼은 그대로 합산에 들어가요
    </p>
  </GpCard>
</template>
