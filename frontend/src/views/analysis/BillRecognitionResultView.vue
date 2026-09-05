<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconDrop from '@/components/ui/icons/IconDrop.vue'
import IconFlame from '@/components/ui/icons/IconFlame.vue'
import IconLightning from '@/components/ui/icons/IconLightning.vue'
import { useAnalysisStore } from '@/stores/analysis'
import { formatMonthOnly, formatUsage, formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()

const targetYearMonth = computed(
  () => route.query.month ?? store.targetMonth?.targetYearMonth ?? '2026-08',
)

const recognizedItems = [
  {
    type: 'electricity',
    label: '전기',
    amount: 43200,
    usage: 287,
    unit: 'kWh',
    precision: 0,
    icon: IconLightning,
    color: 'text-primary',
  },
  {
    type: 'water',
    label: '수도',
    amount: 8900,
    usage: 8.9,
    unit: '㎥',
    precision: 1,
    icon: IconDrop,
    color: 'text-water',
  },
  {
    type: 'gas',
    label: '도시가스',
    amount: 12400,
    usage: 12.4,
    unit: '㎥',
    precision: 1,
    icon: IconFlame,
    color: 'text-gas',
  },
]

function editResult() {
  saveRecognizedDraft()
  router.push({
    path: '/analysis/bills/new',
    query: { month: targetYearMonth.value, mode: 'manual', prefill: 'recognition' },
  })
}

function confirmResult() {
  saveRecognizedDraft()
  store.confirmBillDraft()
  router.push({ path: '/analysis', query: { preview: 'confirmed' } })
}

function saveRecognizedDraft() {
  store.saveBillDraft({
    billingMonth: targetYearMonth.value,
    billType: 'MANAGEMENT',
    inputSource: 'OCR',
    items: recognizedItems.map((item) => ({
      utilityType: item.type.toUpperCase(),
      amount: item.amount,
      usage: item.usage,
      usageUnit: item.unit === '㎥' ? 'm3' : item.unit,
    })),
  })
}
</script>

<template>
  <AppSubLayout back="/analysis/bills/new">
    <header class="mb-5">
      <h1 class="text-title text-ink mt-1 mb-1">인식 결과를 확인해 주세요</h1>
      <p class="text-caption text-muted m-0">고지서에서 읽은 항목을 확인했어요.</p>
    </header>

    <section class="bg-surface rounded-xl p-5 shadow-sm">
      <div class="mb-5">
        <p class="text-caption text-muted mt-0 mb-1">인식된 고지서</p>
        <h2 class="text-section text-ink m-0">{{ formatMonthOnly(targetYearMonth) }} 생활비 고지서</h2>
      </div>

      <ul class="m-0 list-none p-0">
        <li
          v-for="item in recognizedItems"
          :key="item.type"
          class="border-divider flex min-h-14 items-center gap-3 border-t first:border-t-0"
        >
          <component :is="item.icon" :size="20" :class="item.color" />
          <span class="text-body-sm text-ink flex-1">{{ item.label }}</span>
          <div class="text-right">
            <strong class="text-body-strong text-ink block tabular-nums">
              {{ formatWon(item.amount) }}
            </strong>
            <span class="text-caption text-muted tabular-nums">
              {{ formatUsage(item.usage, item.precision, item.unit) }}
            </span>
          </div>
        </li>
      </ul>
    </section>

    <div class="mt-5 grid grid-cols-2 gap-3">
      <button
        type="button"
        class="border-control-border bg-surface text-ink h-(--gp-cta-h) rounded-md border text-button"
        @click="editResult"
      >
        수정하기
      </button>
      <GpButton @click="confirmResult">확정하기</GpButton>
    </div>
  </AppSubLayout>
</template>
