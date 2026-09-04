<script setup>
/*
 * WF-06 평가 기간 목표 (B-4-06)
 *
 * ⚠️ **한 카드가 두 응답을 쓴다.**
 *   goal      ← GET /eco/home 의 `goal` — goalSet · combinedTargetRate · tier · expectedMileage 넷뿐
 *   utilities ← GET /eco/rounds/{roundId}/goal 의 `utilities[]` — 요금별 목표·사용량
 * 시안의 「전기 10% · 도시가스 15% · 수도 5%」 3열은 홈 응답에 없어서 목표 조회를 한 번 더 부른다.
 *
 * `expectedMileage` 는 **M**(마일리지)이고 `expectedSaving` 은 **원**이다. 1M = 1원이라
 * 바꿔 써도 숫자가 같아 그럴듯해 보인다 — 포맷터를 반드시 구분해 쓴다.
 *
 * `displayPrecision` 은 preview 응답에만 있어서 여기서는 `usagePrecision(unit)` 으로 되짚는다.
 */
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import UtilityIcon from './UtilityIcon.vue'
import {
  formatMileage,
  formatPercent,
  formatTier,
  formatUnit,
  formatUsage,
  formatUtilityType,
  usagePrecision,
} from '@/utils/format'

defineProps({
  goal: { type: Object, required: true },
  // 목표 조회가 아직 안 끝났으면 null. 3열만 비고 합산은 그대로 보인다
  utilities: { type: Array, default: null },
})
defineEmits(['edit'])
</script>

<template>
  <GpCard title="평가 기간 목표">
    <template #action>
      <button
        type="button"
        class="text-label text-primary-on-soft cursor-pointer border-0 bg-transparent p-0"
        @click="$emit('edit')"
      >
        수정
      </button>
    </template>

    <div class="flex items-end justify-between gap-3">
      <div>
        <p class="text-caption text-muted mt-0 mb-0.5">합산 감축률</p>
        <p class="text-amount-hero tabular-nums m-0">
          {{ formatPercent(goal.combinedTargetRate) }}
        </p>
      </div>
      <div class="text-right">
        <p class="text-caption text-muted mt-0 mb-0.5 flex items-center justify-end gap-1.5">
          <GpTag tone="estimated" small>예상</GpTag>
          마일리지
        </p>
        <p class="text-amount tabular-nums m-0">{{ formatMileage(goal.expectedMileage) }}</p>
      </div>
    </div>
    <p class="text-caption text-muted mt-1 mb-0">{{ formatTier(goal.tier) }} 구간이에요</p>

    <ul
      v-if="utilities?.length"
      class="border-divider mt-4 mb-0 grid list-none grid-cols-3 gap-2 border-t p-0 pt-4"
    >
      <li v-for="item in utilities" :key="item.utilityType" class="min-w-0">
        <div class="flex items-center gap-1.5">
          <UtilityIcon :utility-type="item.utilityType" small />
          <span class="text-caption text-ink-soft truncate">
            {{ formatUtilityType(item.utilityType) }}
          </span>
        </div>
        <p class="text-list-title tabular-nums mt-1.5 mb-0">{{ formatPercent(item.targetRate) }}</p>
        <p class="text-caption-sm text-muted tabular-nums mt-0.5 mb-0">
          {{ formatUsage(item.baselineUsage, usagePrecision(item.usageUnit)) }}
          →
          {{
            formatUsage(item.targetUsage, usagePrecision(item.usageUnit), formatUnit(item.usageUnit))
          }}
        </p>
      </li>
    </ul>
  </GpCard>
</template>
