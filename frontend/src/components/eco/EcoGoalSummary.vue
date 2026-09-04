<script setup>
/*
 * 목표 합산 요약 — WF-04 (B-2-04 ~ B-2-07)
 *
 * `combined` 와 `utilities` 는 `POST /eco/rounds/{roundId}/goal/preview` 응답 그대로다.
 * **여기서 숫자를 만들지 않는다.** 합산 감축률은 요금별 감축률의 평균이 아니라 탄소 가중이라
 * 화면이 다시 계산하면 서버와 조용히 어긋난다.
 *
 * ── 단위를 바꿔 쓰지 않는다 ──────────────────────────────────────────────
 *   expectedMileage · nextTier.mileage → **M** (formatMileage)
 *   expectedSaving · totalExpectedSaving · baselineTotalAmount → **원** (formatWon)
 * 1M = 1원이라 숫자가 같아 틀려도 그럴듯해 보인다.
 *
 * `expectedMileage` 는 **예상값**이다(핵심 규칙 2 · COM-06). `예상` 라벨을 반드시 붙이고
 * 옆에 전환·출금 버튼을 두지 않는다.
 *
 * `combined.tier` 가 null 이면 5% 미만이라 지급 구간에 못 든다. 0M 을 보여주되
 * 실패가 아니라 "조금 더 줄이면 받을 수 있다"로 읽히게 `nextTier` 를 같이 낸다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import {
  formatMileage,
  formatPercent,
  formatPoint,
  formatUtilityType,
  formatWon,
} from '@/utils/format'

const props = defineProps({
  combined: { type: Object, default: null },
  utilities: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

// 절감액이 있는 요금만 줄로 세운다. 구간을 안 고른 요금은 preview 에 아예 없다
const rows = computed(() => props.utilities.filter((item) => item.expectedSaving != null))

const excludedLabel = computed(() =>
  (props.combined?.excludedUtilities ?? []).map(formatUtilityType).join(' · '),
)
</script>

<template>
  <GpCard v-if="combined" tone="estimated" :class="loading ? 'opacity-60' : ''">
    <div class="flex items-center gap-2">
      <h2 class="text-section tracking-display m-0">예상 마일리지</h2>
      <GpTag tone="estimated">예상</GpTag>
    </div>

    <p class="text-amount-hero tabular-nums mt-3 mb-1">
      {{ formatMileage(combined.expectedMileage) }}
    </p>
    <p class="text-caption text-muted mt-0 mb-4">
      합산 {{ formatPercent(combined.combinedRate) }} 감축
      <template v-if="combined.tierLabel"> · {{ combined.tierLabel }} 구간</template>
      <template v-else> · 5% 미만이라 아직 지급 구간이 아니에요</template>
    </p>

    <div class="border-divider border-t">
      <div
        v-for="row in rows"
        :key="row.utilityType"
        class="flex items-center justify-between gap-3 py-2.5"
      >
        <span class="text-body text-ink-soft">
          {{ formatUtilityType(row.utilityType) }}
          <span class="text-muted tabular-nums">{{ formatPercent(row.targetRate) }}</span>
        </span>
        <span class="text-body-strong tabular-nums">{{ formatWon(row.expectedSaving) }}</span>
      </div>
    </div>

    <div class="border-divider flex items-center justify-between gap-3 border-t pt-3">
      <span class="text-list-title">줄어드는 요금</span>
      <span class="text-amount tabular-nums text-decrease">
        {{ formatWon(combined.totalExpectedSaving) }}
      </span>
    </div>
    <p class="text-caption text-muted mt-1 mb-0 tabular-nums">
      기준 요금 {{ formatWon(combined.baselineTotalAmount) }} 대비
    </p>

    <!-- 다음 구간까지 남은 거리. %p 는 증감이 아니라 두 비율의 차이다 (GpDelta 를 쓰지 않는다) -->
    <p
      v-if="combined.nextTier"
      class="text-caption text-primary-on-soft bg-primary-bg mt-4 mb-0 rounded-sm px-3 py-2.5"
    >
      {{ formatPoint(combined.nextTier.gapPoint) }} 더 줄이면
      {{ formatMileage(combined.nextTier.mileage) }} 구간이에요
    </p>

    <p v-if="excludedLabel" class="text-caption text-muted mt-3 mb-0">
      {{ excludedLabel }}는 등록되지 않아 합산에서 빠졌어요
    </p>
  </GpCard>

  <GpCard v-else tone="sub">
    <p class="text-caption text-muted m-0">구간을 고르면 예상 마일리지가 나와요</p>
  </GpCard>
</template>
