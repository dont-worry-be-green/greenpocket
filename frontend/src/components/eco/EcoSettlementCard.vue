<script setup>
/*
 * 적립된 마일리지 — WF-11 (B-5-03)
 *
 * `settlement` 은 `GET /eco/rounds/{roundId}/settlement` 그대로다.
 *
 * ── ⚠️ 아직 현금이 아니다 ──────────────────────────────────────────────────
 * `isCash: false` 다. 돈의 3단계 중 ② **적립된 마일리지**라 `statusLabel`("확인")을 함께 단다.
 * ③ 그린포켓 입금은 전환을 마쳐야 생긴다 — 여기 숫자를 잔액처럼 쓰지 않는다(핵심 규칙 2·3).
 *
 * ── 전환 버튼은 여기 없다 ──────────────────────────────────────────────────
 * 「현금으로 바꾸기」는 `POST /pocket/conversions` 라 **포켓 도메인**이고, 화면 하단 고정 CTA 라
 * 뷰의 `#footer` 에 있다. 카드 안에 두면 본문 중간에 CTA 가 끼어 뒤 카드가 안 읽힌다.
 *
 * `otherUses` 는 마일리지를 현금 말고 쓸 수 있는 곳이다. 「나중에」를 골라도 포켓 탭에서
 * 전환할 수 있다는 것을 함께 적어 둔다 — 지금 안 하면 사라지는 것으로 읽힌다.
 */
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatMileage, formatRoundPeriod } from '@/utils/format'

defineProps({
  settlement: { type: Object, required: true },
})
</script>

<template>
  <div class="space-y-4">
    <GpCard tone="confirmed">
      <div class="flex items-center justify-between gap-3">
        <span class="text-body text-ink-soft">
          {{ formatRoundPeriod(settlement.periodStart, settlement.periodEnd) }}
        </span>
        <GpTag tone="confirmed">{{ settlement.statusLabel }}</GpTag>
      </div>
      <p class="text-amount tabular-nums mt-3 mb-0">
        {{ formatMileage(settlement.confirmedMileage) }}
      </p>
      <p class="text-caption text-muted mt-2 mb-0">
        <template v-if="settlement.isCash">그린포켓에 들어온 금액이에요</template>
        <template v-else>아직 현금이 아니에요. 현금으로 바꾸면 그린포켓에 들어와요</template>
      </p>
    </GpCard>

    <GpCard title="마일리지로 할 수 있는 것">
      <p class="text-body text-ink-soft mt-0 mb-3">
        현금 말고도 {{ settlement.otherUses.join(' · ') }} 에 쓸 수 있어요.
      </p>
      <a
        :href="settlement.externalUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="text-label text-primary-on-soft no-underline"
      >
        에코마일리지 누리집에서 보기
      </a>
    </GpCard>
  </div>
</template>
