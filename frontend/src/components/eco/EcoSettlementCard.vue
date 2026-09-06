<script setup>
/*
 * 적립된 마일리지 — WF-11 (B-5-03)
 *
 * `settlement` 은 `GET /eco/rounds/{roundId}/settlement` 그대로다.
 *
 * ── ⚠️ 아직 현금이 아니다 ──────────────────────────────────────────────────
 * `isCash: false` 다. 돈의 3단계 중 ② **적립된 마일리지**라 `statusLabel`("확인")을 함께 단다
 * (B-5-03 이 "30,000M 적립됐어요 + 확인 라벨" 로 못 박는다). ③ 그린포켓 입금은 전환을 마쳐야
 * 생긴다 — 여기 숫자를 잔액처럼 쓰지 않는다(핵심 규칙 2·3).
 *
 * ── 전환 버튼은 여기 없다 ──────────────────────────────────────────────────
 * 「현금으로 바꾸기」는 `POST /pocket/conversions` 라 **포켓 도메인**이고, 화면 하단 고정 CTA 라
 * 뷰의 `#footer` 에 있다. 카드 안에 두면 본문 중간에 CTA 가 끼어 뒤 카드가 안 읽힌다.
 *
 * ── GpCard 를 쓰지 않는다 ─────────────────────────────────────────────────
 * 이 카드만 **채운 초록 배경**이다(시안 WF-11). `GpCard` 의 tone 은 흰·회색·연앰버 넷뿐이라
 * 여기에 맞는 것이 없고, 화면 하나를 위해 공용 컴포넌트에 tone 을 늘리지 않는다.
 * `확인` 배지도 같은 이유로 손으로 그린다 — `GpTag` 의 tone 은 전부 밝은 배경 전제다.
 */
import GpDelta from '@/components/ui/GpDelta.vue'
import IconCrown from '@/components/ui/icons/IconCrown.vue'
import { formatMileage, formatRoundPeriod, formatTier } from '@/utils/format'

defineProps({
  settlement: { type: Object, required: true },
})
</script>

<template>
  <section class="bg-primary text-on-primary rounded-lg p-(--gp-card-pad)">
    <div class="text-center">
      <span
        class="bg-confirmed-bg text-confirmed mx-auto flex size-12 items-center justify-center rounded-full"
        aria-hidden="true"
      >
        <IconCrown :size="24" />
      </span>

      <p class="text-caption mt-3 mb-0 opacity-90">
        {{ formatRoundPeriod(settlement.periodStart, settlement.periodEnd) }} 평가가 끝났어요
      </p>
      <p class="text-amount-hero mt-1 mb-0 tabular-nums">
        {{ formatMileage(settlement.confirmedMileage) }}
      </p>
      <p class="text-body-strong mt-0.5 mb-0">적립됐어요</p>

      <!-- 색만으로 구분하지 않는다 (COM-07). 초록 위라 GpTag 대신 같은 모양을 직접 그린다 -->
      <span
        class="bg-surface text-primary-on-soft text-label mt-3 inline-flex h-(--gp-tag-h) items-center rounded-xs px-2 font-semibold"
      >
        {{ settlement.statusLabel }}
      </span>
    </div>

    <dl class="bg-surface text-ink mt-4 mb-0 rounded-md px-3 py-2.5">
      <div class="flex items-center justify-between gap-3 py-1.5">
        <dt class="text-body text-ink-soft">평가 기간 누적</dt>
        <dd class="m-0"><GpDelta :value="settlement.cumulativeRate" size="sm" /></dd>
      </div>
      <div class="border-divider flex items-center justify-between gap-3 border-t py-1.5 pt-2.5">
        <dt class="text-body text-ink-soft">지급 구간</dt>
        <dd class="text-body-strong m-0 tabular-nums">
          {{ formatTier(settlement.tier) }} · {{ formatMileage(settlement.confirmedMileage) }}
        </dd>
      </div>
    </dl>
  </section>
</template>
