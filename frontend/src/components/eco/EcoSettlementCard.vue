<script setup>
/*
 * 적립된 마일리지 헤더 — WF-11 (B-5-03)
 *
 * `settlement` 은 `GET /eco/rounds/{roundId}/settlement` 그대로다.
 *
 * ── 토스 「포인트 적립」 화면 문법 (2026-09-10 수현) ─────────────────────────
 * 파일 이름은 그대로지만 카드가 아니다. 채운 초록 면(시안 WF-11 → 포켓 잔액 문법)은 앱에서
 * 포켓 잔액 한 곳만 쓰는 문법이라 여기 들어오면 덩어리로 튀었다(결정 C-39). 배경 없이
 * **가운데 정렬**로 동전 → 금액 → 문구 순서다. 토스가 이자·포인트를 받는 순간에 쓰는 순서 그대로다.
 *   ① 동전이 위에서 떨어져 한 번 튕긴다(overshoot)          0 ~ 620ms
 *   ② 동전이 닿는 순간부터 금액이 0 에서 올라간다(ease-out)  350 ~ 1150ms
 *   ③ 캡션이 뒤따라 올라온다                                  650ms
 * 토스 원칙대로 **움직이는 것은 한 대상에 한 동작**이고 순서만 타임라인으로 잇는다. 색은 「확인」
 * 앰버 태그와 마크뿐이다. 기각: 채운 초록 그라디언트 카드 · 앰버 틴트 카드 · 흰 카드 · 왼쪽 정렬 헤더.
 *
 * `prefers-reduced-motion` 이면 애니메이션 없이 최종 상태를 바로 그린다. 테스트(jsdom)처럼
 * `requestAnimationFrame` 이 없어도 같다 — 숫자가 0 에 멈춘 채 단언되면 안 된다.
 *
 * ── ⚠️ 아직 현금이 아니다 ──────────────────────────────────────────────────
 * `isCash: false` 다. 돈의 3단계 중 ② **적립된 마일리지**다. ⚠️ `statusLabel`("확인") 배지는
 * **수현 결정으로 뺐다**(2026-09-10 · 결정 C-39) — B-5-03·COM-06 의 「확인 라벨」과 어긋나므로
 * 명세 쪽에 결정으로 적어 뒀다. ②라는 사실은 푸터의 「아직 현금이 아니에요」 노티스가 말한다.
 * ③ 그린포켓 입금은 전환을 마쳐야 생긴다 — 여기 숫자를 잔액처럼 쓰지 않는다(핵심 규칙 2·3).
 *
 * ── 캡션은 기간뿐이다 ────────────────────────────────────────────────────────
 * 「2025.10~2026.03 평가분」. 감축률·구간·덜 낸 요금은 바로 아래 스탯 타일 셋(`EcoSettlementStats`)이
 * 맡는다(2026-09-10 수현). 예전 인셋 두 행과 「사용량 R% 줄여 구간」 캡션은 타일과 중복이라 뺐다.
 *
 * ── 전환 버튼은 여기 없다 ──────────────────────────────────────────────────
 * 「현금으로 바꾸기」는 `POST /pocket/conversions` 라 **포켓 도메인**이고, 화면 하단 고정 CTA 라
 * 뷰의 `#footer` 에 있다.
 *
 * 마크는 홈 감축률 카드 「예상 적립」 줄이 쓰는 에코마일리지 마크(`eco-mileage-mark.png`)다
 * (2026-09-10 수현 — 앰버 원 + 코인 아이콘에서 교체). 애니메이션은 감싼 `<span>` 에 걸려 있다.
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import ecoMark from '@/assets/eco-mileage-mark.png'

import { formatMileage, formatRoundPeriodChip } from '@/utils/format'

const props = defineProps({
  settlement: { type: Object, required: true },
})

/** 금액 카운트업. 동전이 닿는 350ms 뒤에 시작해 800ms 동안 ease-out 으로 올라간다 */
const COUNT_DELAY = 350
const COUNT_DURATION = 800

const shown = ref(0)
const animate = ref(false)
let frame = 0
let timer = 0

const easeOut = (t) => 1 - (1 - t) ** 3

function reducedMotion() {
  return (
    typeof window === 'undefined' ||
    typeof window.requestAnimationFrame !== 'function' ||
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  )
}

function countUp() {
  const target = props.settlement.confirmedMileage ?? 0
  const start = performance.now()
  const step = (now) => {
    const t = Math.min((now - start) / COUNT_DURATION, 1)
    shown.value = Math.round(target * easeOut(t))
    if (t < 1) frame = window.requestAnimationFrame(step)
  }
  frame = window.requestAnimationFrame(step)
}

onMounted(() => {
  if (reducedMotion()) {
    shown.value = props.settlement.confirmedMileage ?? 0
    return
  }
  animate.value = true
  timer = window.setTimeout(countUp, COUNT_DELAY)
})

onBeforeUnmount(() => {
  if (frame) window.cancelAnimationFrame(frame)
  if (timer) window.clearTimeout(timer)
})

const amount = computed(() => formatMileage(shown.value).replace('M', ''))
</script>

<template>
  <header class="pt-6 pb-5 text-center" :class="{ 'is-animated': animate }">
    <!-- 에코마일리지 마크 — 홈 감축률 카드 「예상 적립」 줄과 같은 이미지(eco-mileage-mark.png) -->
    <span class="coin mx-auto flex h-16 items-center justify-center" aria-hidden="true">
      <img :src="ecoMark" alt="" class="h-full w-auto select-none" />
    </span>

    <!-- 「확인」 배지와 「마일리지가 적립됐어요」 문장은 뺐다(2026-09-10 수현 · 결정 C-39) -->
    <p class="mt-5 mb-0 flex items-baseline justify-center gap-0.5 tabular-nums" aria-live="polite">
      <span class="text-display tracking-display text-ink">{{ amount }}</span>
      <span class="text-section text-ink font-extrabold">M</span>
    </p>

    <!-- 감축률·구간·덜 낸 요금은 바로 아래 스탯 타일(EcoSettlementStats)이 맡는다. 여기는 기간만 -->
    <p class="rise rise-2 text-caption text-muted mt-2 mb-0 tabular-nums">
      {{ formatRoundPeriodChip(settlement.periodStart, settlement.periodEnd) }} 평가분
    </p>
  </header>
</template>

<style scoped>
/* 마크: 위에서 떨어져 한 번 튕긴다. 60% 에서 살짝 넘치고 80% 에서 되돌아온다 */
@keyframes coin-drop {
  0% {
    opacity: 0;
    transform: translateY(-44px) scale(0.6);
  }
  60% {
    opacity: 1;
    transform: translateY(8px) scale(1.06);
  }
  80% {
    transform: translateY(-4px) scale(0.98);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

/* 문구: 아래에서 살짝 올라오며 나타난다 */
@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.is-animated .coin {
  animation: coin-drop 620ms var(--ease-enter) both;
}

.is-animated .rise {
  animation: rise-in var(--gp-duration-base) var(--ease-enter) both;
}
.is-animated .rise-1 {
  animation-delay: 500ms;
}
.is-animated .rise-2 {
  animation-delay: 650ms;
}

@media (prefers-reduced-motion: reduce) {
  .is-animated .coin,
  .is-animated .rise {
    animation: none;
  }
}
</style>
