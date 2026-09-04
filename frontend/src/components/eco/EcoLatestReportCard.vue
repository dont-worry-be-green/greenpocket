<script setup>
/*
 * WF-06 전달 리포트 요약 (B-4-02)
 *
 * `report` 는 GET /eco/home 의 `latestReport` 그대로다.
 *
 * ⚠️ **`available: false` 는 오류가 아니다**(핵심 규칙 8). 아직 그 달 고지서를 올리지 않았다는
 * 뜻이라 에러 문구가 아니라 안내를 그린다. 서버는 200 으로 답한다.
 *
 * ⚠️ `monthlyRate` 는 **그 달 하나**의 감축률이고 `targetRate` 는 회차 전체 목표다.
 * 누적률(`progress.cumulativeRate`)과 섞지 않는다 — 같은 화면에서 숫자가 갈린다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import { formatDateTime, formatMonth, formatPercent } from '@/utils/format'

const props = defineProps({
  report: { type: Object, required: true },
})
defineEmits(['detail'])

const badge = computed(() =>
  props.report.available ? `${formatMonth(props.report.reportMonth)} 고지서` : '',
)

const registeredLabel = computed(() =>
  props.report.billRegisteredAt ? `${formatDateTime(props.report.billRegisteredAt)} 등록` : '',
)

const verdict = computed(() =>
  props.report.achieved
    ? '이 달은 목표를 넘겼어요'
    : `이 달만 보면 목표 ${formatPercent(props.report.targetRate)}에 못 미쳤어요`,
)
</script>

<template>
  <GpCard title="전달 리포트" :badge="badge">
    <template v-if="report.available" #action>
      <button
        type="button"
        class="text-label text-primary-on-soft inline-flex cursor-pointer items-center gap-0.5 border-0 bg-transparent p-0"
        @click="$emit('detail')"
      >
        자세히
        <IconChevronRight :size="16" />
      </button>
    </template>

    <template v-if="report.available">
      <GpDelta :value="report.monthlyRate" size="lg" />
      <p class="text-body text-ink-soft mt-2 mb-0">{{ verdict }}</p>
      <p v-if="registeredLabel" class="text-caption text-muted mt-1 mb-0">{{ registeredLabel }}</p>
    </template>

    <!-- 고지서가 아직 없는 달. 숫자를 만들지 않는다(핵심 규칙 8) -->
    <p v-else class="text-body text-ink-soft m-0">
      아직 올린 고지서가 없어요. 진단 탭에서 고지서를 등록하면 그 달의 결과를 알려드려요.
    </p>
  </GpCard>
</template>
