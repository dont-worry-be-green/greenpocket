<script setup>
/*
 * 요금 비교 — 기준 요금 · 이번 평가 요금 가로 막대 둘 — WF-11 (B-5-03)
 *
 * ── 필드명이 두 응답에서 다르다 ────────────────────────────────────────────
 * `result.amount` 는 `baselineTotal · actualTotal`, `settlement.calculation` 은
 * `baselineAmount · actualAmount` 다. 같은 두 숫자라 막대는 하나면 되는데 이름이 달라,
 * **객체가 아니라 숫자 두 개를 받는다.** 여기서 두 이름을 다 뒤지면 오타가 나도 `undefined` 라
 * 조용히 빈칸이 된다. 차액(`savedAmount`)은 여기 없다 — 헤더 아래 스탯 타일(`EcoSettlementStats`)이
 * 맡는다(2026-09-10 수현). 그래서 이 카드는 **막대 둘 + 기준선 문장**뿐이다.
 *
 * ── 표가 아니라 「막대 둘」이다 (2026-09-09 수현) ──────────────────────────────
 * 라벨 왼쪽·숫자 오른쪽 dt/dd 표는 눈이 한 줄을 가로질러야 짝이 맞고, 셋째 줄 「줄인 금액」이
 * 같은 굵기로 묻혔다. Opower 홈 에너지 리포트·토스 소비 리포트·우리 진단 탭 A-3-06 이 다 같은
 * 문법이다 — **막대 두 개가 먼저, 숫자는 막대 끝에.** 진단 탭(`AnalysisHomeView` 작년 비교
 * 카드)의 막대 색(회색 `icon-off` = 기준, 브랜드 초록 `primary-soft` = 이번)을 그대로 가져와
 * 앱 안에서 「비교」가 한 모양이 되게 한다. 막대는 가로다 — 카드 폭 321 에 항목 둘이라
 * 세로 그룹 막대보다 숫자 라벨 자리가 넉넉하다.
 * 기각: dt/dd 표 · 우상단 차액 칩 · 구분선 아래 차액 행(스탯 타일과 중복).
 *
 * ── ⚠️ 이 카드에 CTA 도 큰 숫자도 없다 (핵심 규칙 2·3) ────────────────────────
 * 요금은 **포켓 잔액이 아니다.** 덜 냈다는 성과 표시일 뿐이라 전환·출금 대상이 아니다
 * (`savedIsPocketEligible: false`). 전환으로 가는 길은 화면 하단 고정 버튼 하나뿐이다.
 *
 * ── ⚠️ 「어떻게 계산됐나요」라고 부르지 않는다 (결정 C-38) ─────────────────────
 * 마일리지 구간은 **사용량(탄소 환산) 감축률 R** 로 정해지고(계산식 5~7) 요금은 판정에 안 들어간다.
 * 이 카드를 「계산」이라 부르면 「50,500원 줄여서 30,000M」으로 읽힌다. 픽스처만 봐도 요금은 12.0%,
 * 감축률은 12.499% 로 다른 값이다. 제목은 「덜 낸 요금」(원화 우선 · 핵심 규칙 1·7)이다.
 *
 * 마일리지(M)가 아니라 **원**이다. 1M = 1원이라 숫자가 같아 바꿔 써도 그럴듯해 보인다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import { formatWon } from '@/utils/format'

const props = defineProps({
  title: { type: String, default: '덜 낸 요금' },
  baseline: { type: Number, required: true },
  actual: { type: Number, required: true },
  /** `calculation.note` 처럼 서버가 문장으로 준 기준선 설명. 막대 아래에 근거로 붙는다 */
  note: { type: String, default: '' },
})

/** 가장 긴 막대가 100%. 0 원 항목도 막대 자리는 남기도록 최소 폭을 둔다 */
const MIN_WIDTH = 4
const widthOf = (amount) => {
  const max = Math.max(props.baseline, props.actual, 0)
  if (max <= 0) return MIN_WIDTH
  return Math.max((amount / max) * 100, MIN_WIDTH)
}

const bars = computed(() => [
  { key: 'baseline', label: '기준 요금', amount: props.baseline, fill: 'bg-icon-off', text: 'text-ink-soft' },
  { key: 'actual', label: '이번 평가 요금', amount: props.actual, fill: 'bg-primary-soft', text: 'text-ink' },
])

</script>

<template>
  <GpCard :title="title">
    <!-- 기준(회색) 위, 이번(초록) 아래. 막대 길이로 「줄었다」를 먼저 읽고 숫자는 막대 끝에서 확인한다 -->
    <ul class="m-0 list-none space-y-2.5 p-0">
      <li v-for="bar in bars" :key="bar.key" class="flex items-center gap-2.5">
        <span class="text-caption text-muted w-[88px] shrink-0 font-semibold">{{ bar.label }}</span>
        <span class="min-w-0 flex-1">
          <i
            class="block h-4 rounded-xs"
            :class="bar.fill"
            :style="{ width: `${widthOf(bar.amount)}%` }"
            role="img"
            :aria-label="`${bar.label} ${formatWon(bar.amount)}`"
          />
        </span>
        <span class="text-body-strong w-[78px] shrink-0 text-right tabular-nums" :class="bar.text">
          {{ formatWon(bar.amount) }}
        </span>
      </li>
    </ul>

    <!-- 무엇과 비교한 값인지 서버가 문장으로 준다 (핵심 규칙 7) -->
    <p v-if="note" class="border-divider text-caption text-muted mt-3.5 mb-0 border-t pt-3">
      {{ note }}
    </p>
  </GpCard>
</template>
