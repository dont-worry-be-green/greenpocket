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
 * 여기 마일리지는 **확정된 값**이다. 아직 현금은 아니므로 **모달에서 전환·출금으로
 * 보내지 않는다** — 적립 화면(WF-11)을 거친다.
 *
 * ⚠️ `확인` 배지를 달지 않는다(시안 WF-09 · 수현 결정). COM-06 의 상태 라벨 규칙은
 * 아래 캡션("…지금 확정됐어요")과, 이 모달이 데려가는 **결과 화면(WF-10)의 `확인` 배지**가
 * 받는다. 알림 한 장에 라벨까지 얹으면 정작 읽어야 할 두 숫자가 묻힌다.
 *
 * 「나중에 볼게요」도 닫기와 같은 `close` 를 낸다. 닫으면 서버에 봤다고 알려
 * 다시 뜨지 않게 하는 것이 완료 조건이라, 두 경로가 갈리면 한쪽만 안 알린다.
 *
 * ⚠️ **GpModal 에 title 을 넘기지 않는다.** 시안은 제목이 헤더 왼쪽이 아니라 본문 가운데
 * 아이콘 아래에 온다. title 을 주면 같은 문장이 두 번 보인다.
 */
import GpButton from '@/components/ui/GpButton.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpModal from '@/components/ui/GpModal.vue'
import IconCrown from '@/components/ui/icons/IconCrown.vue'
import { formatMileage, formatRoundPeriod } from '@/utils/format'

defineProps({
  modal: { type: Object, default: null },
  open: { type: Boolean, default: false },
})
defineEmits(['close', 'view'])
</script>

<template>
  <GpModal :open="open && modal !== null" align="center" @close="$emit('close')">
    <div class="text-center">
      <span
        class="bg-confirmed-bg text-confirmed mx-auto flex size-12 items-center justify-center rounded-full"
        aria-hidden="true"
      >
        <IconCrown :size="24" />
      </span>

      <h2 class="text-section tracking-display text-ink mt-3 mb-1">지난 평가 결과가 나왔어요</h2>
      <p class="text-caption text-muted mt-0 mb-0">
        {{ formatRoundPeriod(modal.periodStart, modal.periodEnd) }} 평가분
      </p>
    </div>

    <div class="bg-positive-bg mt-4 flex items-center justify-between gap-3 rounded-md px-3 py-3.5">
      <GpDelta :value="modal.finalRate" size="lg" />
      <span class="text-amount text-ink tabular-nums">{{ formatMileage(modal.mileage) }}</span>
    </div>

    <p class="text-caption text-muted mt-3 mb-0 text-center">
      평가 기간이 끝나고 검침이 다 반영돼 지금 확정됐어요
    </p>

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
