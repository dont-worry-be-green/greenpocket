<script setup>
/*
 * 목표 합산 요약 — WF-04 (B-2-07). 목표 카드(EcoGoalPlanner) 아래쪽에 들어가는 블록이다.
 *
 * `combined` 는 `POST /eco/rounds/{roundId}/goal/preview` 응답의 `combined` 그대로다.
 * **여기서 숫자를 만들지 않는다.** 합산 감축률은 요금별 감축률의 평균이 아니라 탄소 가중이라
 * 화면이 다시 계산하면 서버와 조용히 어긋난다. 요금별 절감액은 카드 위 행이 보여주므로 여기는
 * 합계(줄어드는 요금)와 예상 마일리지만 둔다.
 *
 * ── 단위를 바꿔 쓰지 않는다 ──────────────────────────────────────────────
 *   expectedMileage → **M** (formatMileage) · totalExpectedSaving → **원** (formatWon)
 * 1M = 1원이라 숫자가 같아 틀려도 그럴듯해 보인다.
 *
 * `expectedMileage` 는 **예상값**이다(핵심 규칙 2 · COM-06). `예상` 라벨과 estimated 톤을 반드시
 * 붙이고 옆에 전환·출금 버튼을 두지 않는다. 마일리지(M)가 나오는 곳은 이 화면에서 여기뿐이다(C-37).
 *
 * `combined.tier` 가 null 이면 5% 미만이라 지급 구간에 못 든다. 0M 과 「아직 지급 구간이 아니에요」로
 * 보여준다. 「기준 요금 대비」 캡션과 「N%p 더 줄이면」 안내(`nextTier`)는 뺐다(2026-09-09 수현).
 */
import { computed } from 'vue'

import GpTag from '@/components/ui/GpTag.vue'
import { formatMileage, formatPercent, formatUtilityType, formatWon } from '@/utils/format'

const props = defineProps({
  combined: { type: Object, default: null },
  loading: { type: Boolean, default: false },
})

const excludedLabel = computed(() =>
  (props.combined?.excludedUtilities ?? []).map(formatUtilityType).join(' · '),
)
</script>

<template>
  <div :class="loading ? 'opacity-60' : ''">
    <div class="border-divider flex items-center justify-between gap-3 border-t pt-3.5">
      <span class="text-list-title">줄어드는 요금</span>
      <span class="text-amount tabular-nums" :class="combined ? 'text-decrease' : 'text-icon-off'">
        {{ combined ? formatWon(combined.totalExpectedSaving) : '0원' }}
      </span>
    </div>

    <div class="bg-estimated-bg mt-3.5 rounded-lg px-4 pt-3.5 pb-4">
      <div class="flex items-center gap-2">
        <span class="text-list-title">예상 마일리지</span>
        <GpTag tone="estimated">예상</GpTag>
      </div>
      <template v-if="combined">
        <p class="text-amount-hero tabular-nums mt-1.5 mb-0.5">
          {{ formatMileage(combined.expectedMileage) }}
        </p>
        <p class="text-caption text-muted m-0">
          합산 {{ formatPercent(combined.combinedRate) }} 감축
          <template v-if="combined.tierLabel"> · {{ combined.tierLabel }} 구간</template>
          <template v-else> · 5% 미만이라 아직 지급 구간이 아니에요</template>
        </p>
      </template>
      <p v-else class="text-caption text-muted mt-1.5 mb-0">목표를 고르면 예상 마일리지가 나와요</p>
    </div>

    <p class="text-caption text-muted mt-3 mb-0">
      마일리지는 세 요금을 온실가스로 환산해 합친 감축률로 정해져요<template v-if="excludedLabel">
        · {{ excludedLabel }}는 등록되지 않아 합산에서 빠졌어요</template
      >
    </p>
  </div>
</template>
