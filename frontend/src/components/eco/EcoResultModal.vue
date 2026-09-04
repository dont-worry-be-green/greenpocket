<script setup>
/*
 * 결산 알림 모달 — WF-09 (B-5-01)
 *
 * `modal` 은 `GET /eco/home` 의 `resultModal` 그대로다.
 *
 * ⚠️ **`modal.roundId` 는 지난 회차다.** 홈이 보여 주는 진행 중 회차가 아니라,
 * 방금 확정된 직전 회차다. 결과 화면으로 보낼 때 이 번호를 그대로 쓴다.
 *
 * ── 돈의 3단계 중 ② ────────────────────────────────────────────────────────
 * 여기 마일리지는 **확정된 값**이라 `확인` 태그를 단다(COM-06). 아직 현금은 아니므로
 * **모달에서 전환·출금으로 보내지 않는다** — 적립 화면(WF-11)을 거친다.
 *
 * 「나중에 볼게요」도 닫기와 같은 `close` 를 낸다. 닫으면 서버에 봤다고 알려
 * 다시 뜨지 않게 하는 것이 완료 조건이라, 두 경로가 갈리면 한쪽만 안 알린다.
 */
import { computed } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpModal from '@/components/ui/GpModal.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatMileage, formatRoundPeriod, formatTier } from '@/utils/format'

const props = defineProps({
  modal: { type: Object, default: null },
  open: { type: Boolean, default: false },
})
defineEmits(['close', 'view'])

/** 서버가 `tierLabel` 을 주지 않는 응답이라 `formatTier` 로 만든다 */
const tierLabel = computed(() => formatTier(props.modal?.tier))
</script>

<template>
  <GpModal :open="open && modal !== null" title="평가 결과가 나왔어요" @close="$emit('close')">
    <p class="text-caption text-muted mt-0 mb-3">
      {{ formatRoundPeriod(modal.periodStart, modal.periodEnd) }} 평가가 끝났어요
    </p>

    <div class="bg-confirmed-bg rounded-md p-3">
      <div class="flex items-center justify-between gap-3">
        <span class="text-body text-ink-soft">최종 감축률</span>
        <GpDelta :value="modal.finalRate" size="lg" :show-word="false" />
      </div>
      <div class="border-divider mt-3 flex items-center justify-between gap-3 border-t pt-3">
        <span class="text-body text-ink-soft">적립된 마일리지</span>
        <span class="inline-flex items-center gap-2">
          <GpTag tone="confirmed">확인</GpTag>
          <span class="text-list-title tabular-nums">{{ formatMileage(modal.mileage) }}</span>
        </span>
      </div>
      <p class="text-caption text-muted mt-2 mb-0">{{ tierLabel }} 구간이에요</p>
    </div>

    <template #footer>
      <GpButton @click="$emit('view')">결과 보러 가기</GpButton>
      <button
        type="button"
        class="text-label text-muted mt-2 w-full cursor-pointer border-0 bg-transparent p-2"
        @click="$emit('close')"
      >
        나중에 볼게요
      </button>
    </template>
  </GpModal>
</template>
