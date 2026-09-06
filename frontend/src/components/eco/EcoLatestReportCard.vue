<script setup>
/*
 * WF-06 전달 리포트 요약 (B-4-04)
 *
 * `report` 는 GET /eco/home 의 `latestReport` 그대로다.
 *
 * ⚠️ **`available: false` 는 오류가 아니다**(핵심 규칙 8). 아직 그 달 고지서를 올리지 않았다는
 * 뜻이라 에러 문구가 아니라 안내를 그린다. 서버는 200 으로 답한다.
 *
 * ⚠️ `monthlyRate` 는 **그 달 하나**의 감축률이고 `targetRate` 는 회차 전체 목표다.
 * 누적률(`progress.cumulativeRate`)과 섞지 않는다 — 같은 화면에서 숫자가 갈린다.
 *
 * ── 목표 대비는 문장이 아니라 배지다 ──────────────────────────────────────
 * B-4-04 가 "목표 대비 배지(목표 10% 미달 / 목표 달성)" 로 못 박는다. 한 줄 문장으로 풀면
 * 감축률 옆에서 같은 말을 두 번 하게 되어 카드가 길어진다.
 */
import { computed } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatMonthDay, formatMonth, formatPercent } from '@/utils/format'

const props = defineProps({
  report: { type: Object, required: true },
})
defineEmits(['detail'])

/** '2026년 7월 고지서 · 8월 3일 등록' — 어느 달을 무엇으로 봤는지 한 줄에 담는다 */
const metaLabel = computed(() => {
  const parts = [`${formatMonth(props.report.reportMonth)} 고지서`]
  if (props.report.billRegisteredAt) {
    parts.push(`${formatMonthDay(props.report.billRegisteredAt)} 등록`)
  }
  return parts.join(' · ')
})

const verdict = computed(() =>
  props.report.achieved
    ? { label: '목표 달성', tone: 'positive' }
    : { label: `목표 ${formatPercent(props.report.targetRate)} 미달`, tone: 'sub' },
)
</script>

<template>
  <GpCard title="전달 리포트">
    <template v-if="report.available" #action>
      <GpButton variant="pill" size="pill" @click="$emit('detail')">자세히</GpButton>
    </template>

    <template v-if="report.available">
      <p class="text-caption text-muted mt-0 mb-2">{{ metaLabel }}</p>
      <div class="flex items-center justify-between gap-3">
        <GpDelta :value="report.monthlyRate" size="lg" />
        <GpTag :tone="verdict.tone">{{ verdict.label }}</GpTag>
      </div>
    </template>

    <!-- 고지서가 아직 없는 달. 숫자를 만들지 않는다(핵심 규칙 8) -->
    <p v-else class="text-body text-ink-soft m-0">
      아직 올린 고지서가 없어요. 진단 탭에서 등록하면 그 달 결과를 알려드려요.
    </p>
  </GpCard>
</template>
