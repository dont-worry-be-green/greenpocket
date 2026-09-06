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
 *
 * ── 합산은 큰 숫자가 아니라 제목 옆 배지다 ────────────────────────────────
 * B-4-06 이 "합산 목표(합쳐 11.322%)" 를 요약으로 두고 3열을 본문으로 삼는다. 합산을 히어로
 * 숫자로 키우면 바로 위 진행 카드의 누적 감축률과 큰 숫자가 두 개가 되어 어느 쪽이 지금인지
 * 헷갈린다. 예상 마일리지도 같은 이유로 하단 캡션 한 줄에 넣었다.
 *
 * ⚠️ 시안의 「합쳐 10.5%」는 계산과 맞지 않는 값이다(결정 C-13). 서버가 준 값을 그대로 쓴다.
 */
import { computed } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
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

const props = defineProps({
  goal: { type: Object, required: true },
  // 목표 조회가 아직 안 끝났으면 null. 3열만 비고 합산 배지는 그대로 보인다
  utilities: { type: Array, default: null },
})
defineEmits(['edit'])

const combinedBadge = computed(() => `합쳐 ${formatPercent(props.goal.combinedTargetRate)}`)

/** '1,340 → 1,206'. 단위는 좁은 3열에서 줄을 넘기려고 따로 뗀다 */
function usageChange(item) {
  const precision = usagePrecision(item.usageUnit)
  return `${formatUsage(item.baselineUsage, precision)} → ${formatUsage(item.targetUsage, precision)}`
}
</script>

<template>
  <GpCard title="평가 기간 목표" :badge="combinedBadge">
    <template #action>
      <GpButton variant="pill" size="pill" @click="$emit('edit')">수정</GpButton>
    </template>

    <ul v-if="utilities?.length" class="m-0 grid list-none grid-cols-3 gap-2 p-0">
      <li
        v-for="item in utilities"
        :key="item.utilityType"
        class="bg-surface-sub min-w-0 rounded-md px-2 py-3 text-center"
      >
        <UtilityIcon :utility-type="item.utilityType" small class="mx-auto" />
        <p class="text-caption text-muted mt-2 mb-0 truncate">
          {{ formatUtilityType(item.utilityType) }}
        </p>
        <p class="text-list-title text-primary-on-soft mt-0.5 mb-0 tabular-nums">
          {{ formatPercent(item.targetRate) }}
        </p>
        <p class="text-caption-sm text-muted mt-1 mb-0 tabular-nums">{{ usageChange(item) }}</p>
        <p class="text-caption-sm text-muted mt-0 mb-0">{{ formatUnit(item.usageUnit) }}</p>
      </li>
    </ul>

    <p v-else class="text-body text-muted m-0">불러오는 중이에요…</p>

    <p class="text-caption text-muted mt-3 mb-0 flex flex-wrap items-center gap-1.5">
      <GpTag tone="estimated" small>예상</GpTag>
      온실가스로 환산해 합친 값이에요 · {{ formatTier(goal.tier) }} 구간이면
      {{ formatMileage(goal.expectedMileage) }}
    </p>
  </GpCard>
</template>
