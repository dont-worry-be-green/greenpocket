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
 * ── 본문 두 숫자 (2026-09-09 수현) ──────────────────────────────────────────
 * 한 줄에 **왼쪽 「12.5% 줄였어요」 · 오른쪽 「30,000M」**(여백 균등). 화살표(`GpDelta`)는 쓰지 않는다 —
 * 「줄였어요」가 이미 방향을 말하고, 알림 한 장에서 화살표는 소음이다. 감축률의 자릿수·방향은
 * `changeRateParts` 한 곳에서 받는다(컴포넌트가 숫자를 직접 다듬지 않는다).
 * 제목은 「평가 결과가 나왔어요」 — 「지난」은 뺐다. 기간은 제목 아래 **칩**(홈 감축률 카드
 * 우상단 「2026.04~09」 와 같은 문법 · `formatRoundPeriodChip`)이 말한다. 회색 캡션은 묻혔다.
 * 아이콘 자리(`IconCoins`)는 디자이너 코인 PNG 가 오면 `<img>` 로 바꾼다.
 *
 * 버튼은 나란히 두 개 — **왼쪽 「나중에 볼게요」(배경 없는 회색 글자) · 오른쪽 「결과 보러 가기」(채움, 52px)**.
 * 주 동작이 엄지 쪽(오른쪽)에 온다.
 *
 * 「나중에 볼게요」도 닫기와 같은 `close` 를 낸다. 닫으면 서버에 봤다고 알려
 * 다시 뜨지 않게 하는 것이 완료 조건이라, 두 경로가 갈리면 한쪽만 안 알린다.
 *
 * ⚠️ **GpModal 에 title 을 넘기지 않는다.** 시안은 제목이 헤더 왼쪽이 아니라 본문 가운데
 * 아이콘 아래에 온다. title 을 주면 같은 문장이 두 번 보인다.
 */
import { computed } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpModal from '@/components/ui/GpModal.vue'
import IconCoins from '@/components/ui/icons/IconCoins.vue'
import { changeRateParts, formatMileage, formatRoundPeriodChip } from '@/utils/format'

const props = defineProps({
  modal: { type: Object, default: null },
  open: { type: Boolean, default: false },
})
defineEmits(['close', 'view'])

const rate = computed(() => changeRateParts(props.modal?.finalRate))
</script>

<template>
  <GpModal :open="open && modal !== null" align="center" @close="$emit('close')">
    <div class="text-center">
      <span
        class="bg-confirmed-bg text-confirmed mx-auto flex size-14 items-center justify-center rounded-full"
        aria-hidden="true"
      >
        <IconCoins :size="28" />
      </span>

      <h2 class="text-section tracking-display text-ink mt-3 mb-2">평가 결과가 나왔어요</h2>
      <!-- 홈 감축률 카드 우상단 기간 칩과 같은 문법 -->
      <span
        class="bg-surface-sub text-caption-sm text-ink-soft inline-block rounded-full px-2.5 py-[5px] font-bold tabular-nums"
      >
        {{ formatRoundPeriodChip(modal.periodStart, modal.periodEnd) }} 평가분
      </span>
    </div>

    <!-- 왼쪽 감축률 · 오른쪽 마일리지(확정 ②). 왼·가운데·오른쪽 여백을 같게(justify-evenly) -->
    <div
      class="bg-positive-bg mt-4 flex items-baseline justify-evenly rounded-md py-4 tabular-nums"
    >
      <p class="m-0">
        <template v-if="rate.direction === 'down' || rate.direction === 'up'">
          <b
            class="text-list-title tracking-body"
            :class="rate.direction === 'down' ? 'text-decrease' : 'text-increase'"
          >
            {{ rate.value }}%
          </b>
          <span class="text-caption text-ink-soft ml-1 font-semibold">
            {{ rate.direction === 'down' ? '줄였어요' : '늘었어요' }}
          </span>
        </template>
        <span v-else class="text-body-strong text-muted">{{ rate.word || '-' }}</span>
      </p>
      <b class="text-amount text-ink tracking-body">{{ formatMileage(modal.mileage) }}</b>
    </div>

    <p class="text-caption text-muted mt-3 mb-0 text-center">
      평가 기간이 끝나고 검침이 다 반영돼 지금 확정됐어요
    </p>

    <!-- 왼쪽은 배경 없는 회색 글자, 오른쪽 채움 CTA 는 기본(46)보다 조금 높게 -->
    <template #footer>
      <div class="flex items-stretch gap-2">
        <button
          type="button"
          class="text-body-strong text-muted flex-1 cursor-pointer border-0 bg-transparent p-0"
          @click="$emit('close')"
        >
          나중에 볼게요
        </button>
        <GpButton class="min-h-[52px] flex-1" @click="$emit('view')">결과 보러 가기</GpButton>
      </div>
    </template>
  </GpModal>
</template>
