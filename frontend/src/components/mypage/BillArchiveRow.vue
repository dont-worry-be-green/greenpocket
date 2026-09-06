<script setup>
/*
 * 고지서 한 줄 — MY-03 (A-2-12)
 *
 * ⚠️ **누를 수 없다.** 시안에는 오른쪽 화살표가 있지만 그게 가리키는 상세 화면(AN-08 · A-2-13)
 * 은 아직 없다. 화살표만 두면 눌러도 아무 일이 없는 줄이 되므로 **빼 두었다** — 상세가 붙을 때
 * 화살표와 `@click` 을 같이 넣는다. `GET /bills/{recordId}` 는 백엔드에 이미 있다.
 *
 * 그래서 `<button>` 이 아니라 `<li>` 다. 누를 수 없는 것을 버튼으로 그리지 않는다.
 *
 * 사용량보다 금액이 먼저·크게 온다(frontend/AGENTS.md 6절 · 핵심 규칙 1).
 */
import GpTag from '@/components/ui/GpTag.vue'
import {
  formatBillType,
  formatDotDate,
  formatMonth,
  formatRecordStatus,
  formatUnit,
  formatUsage,
  formatWon,
  usagePrecision,
} from '@/utils/format'

import BillUtilityIcon from './BillUtilityIcon.vue'

const props = defineProps({
  bill: { type: Object, required: true },
})

/** '2026년 9월 · 전기 고지서' */
const title = () => `${formatMonth(props.bill.billingMonth)} · ${formatBillType(props.bill.billType)} 고지서`

/**
 * '210kWh · 31,540원'.
 * `displayPrecision` 은 이 응답에 없다(6.6). 단위에서 되짚는다 — `usagePrecision` 주석 참고.
 */
const usage = () =>
  formatUsage(
    props.bill.usage,
    usagePrecision(props.bill.usageUnit),
    formatUnit(props.bill.usageUnit),
  )
</script>

<template>
  <li class="flex items-center gap-3 py-3.5">
    <BillUtilityIcon :utility-type="bill.utilityType" />

    <div class="min-w-0 flex-1">
      <p class="text-list-title text-ink m-0 truncate">{{ title() }}</p>
      <p class="text-body-sm text-ink-soft mt-0.5 mb-0 tabular-nums">
        {{ usage() }} · {{ formatWon(bill.amount) }}
      </p>
      <p class="text-caption text-muted mt-0.5 mb-0 tabular-nums">
        등록일 {{ formatDotDate(bill.registeredAt) }}
      </p>
    </div>

    <!-- 색만으로 구분하지 않고 라벨을 함께 단다 (COM-06) -->
    <GpTag :tone="bill.recordStatus === 'CONFIRMED' ? 'sub' : 'estimated'" small>
      {{ formatRecordStatus(bill.recordStatus) }}
    </GpTag>
  </li>
</template>
