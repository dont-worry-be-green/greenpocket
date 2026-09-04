<script setup>
/*
 * 평가 결과 요약 — WF-10 (B-5-02)
 *
 * `result` 는 `GET /eco/rounds/{roundId}/result` 그대로다.
 *
 * ── 확정된 값이다 ──────────────────────────────────────────────────────────
 * `confirmedMileage` 는 돈의 3단계 중 ② **적립된 마일리지**다. `예상` 이 아니라 `확인` 태그를 단다.
 * 아직 현금이 아니므로(③ 은 포켓 입금) 여기에 전환·출금 버튼을 두지 않는다(핵심 규칙 3).
 *
 * ── 근거를 함께 둔다 (핵심 규칙 7 · 10) ──────────────────────────────────
 * `confirmedSource`("에코마일리지 누리집 기준")와 `confirmedAt` 을 숫자 옆에 남긴다.
 * 월 페이스는 진단 탭 고지서로 재지만 **최종 확정은 누리집 기준**이라, 화면의 다른 숫자와
 * 다를 수 있다는 것을 여기서 밝혀 두지 않으면 어느 쪽이 맞는지 알 수 없다.
 *
 * ── 미달을 벌주지 않는다 ───────────────────────────────────────────────────
 * `achieved: false` 에 빨간 X 를 쓰지 않는다(api-spec.md 11.1). 목표를 적어 두고
 * "못 미쳐도 줄인 만큼은 합산에 들어가요"를 함께 둔다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconSealCheck from '@/components/ui/icons/IconSealCheck.vue'
import { formatDateTime, formatMileage, formatPercent, formatTier } from '@/utils/format'

const props = defineProps({
  result: { type: Object, required: true },
})

/** 서버가 `tierLabel` 을 주면 그쪽이 우선이다. `formatTier` 는 없을 때의 대체재다 */
const tierLabel = computed(() => props.result.tierLabel || formatTier(props.result.tier))
</script>

<template>
  <GpCard tone="confirmed">
    <div class="flex items-center gap-2">
      <IconSealCheck :size="18" class="text-on-confirmed flex-none" />
      <span class="text-section tracking-display">평가가 확정됐어요</span>
    </div>

    <div class="mt-4 flex items-baseline justify-between gap-3">
      <span class="text-body text-ink-soft">최종 감축률</span>
      <GpDelta :value="result.finalRate" size="lg" :show-word="false" />
    </div>

    <p class="text-caption text-muted mt-1 mb-0">
      <template v-if="result.achieved">
        목표 {{ formatPercent(result.targetRate) }} 줄이기를 넘었어요
      </template>
      <template v-else>
        목표는 {{ formatPercent(result.targetRate) }} 줄이기였어요. 못 미쳐도 줄인 만큼은 합산에
        들어가요
      </template>
    </p>

    <div class="border-divider mt-4 flex items-center justify-between gap-3 border-t pt-4">
      <span class="text-body text-ink-soft">적립된 마일리지</span>
      <span class="inline-flex items-center gap-2">
        <GpTag tone="confirmed">확인</GpTag>
        <span class="text-amount tabular-nums">{{ formatMileage(result.confirmedMileage) }}</span>
      </span>
    </div>
    <p class="text-caption text-muted mt-1 mb-0 text-right">{{ tierLabel }} 구간</p>

    <p class="text-caption-sm text-muted mt-4 mb-0">
      {{ result.confirmedSource }} · {{ formatDateTime(result.confirmedAt) }} 확정
    </p>
  </GpCard>
</template>
