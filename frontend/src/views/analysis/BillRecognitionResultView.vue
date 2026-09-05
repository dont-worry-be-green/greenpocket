<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import IconDrop from '@/components/ui/icons/IconDrop.vue'
import IconFlame from '@/components/ui/icons/IconFlame.vue'
import IconLightning from '@/components/ui/icons/IconLightning.vue'
import { useAnalysisStore } from '@/stores/analysis'
import { formatMonthOnly, formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()

const targetYearMonth = computed(
  () => route.query.month ?? store.targetMonth?.targetYearMonth ?? '2026-08',
)

const recognizedItems = [
  { type: 'electricity', label: '전기', amount: 43200, icon: IconLightning, color: 'text-primary' },
  { type: 'water', label: '수도', amount: 8900, icon: IconDrop, color: 'text-[#448bc9]' },
  { type: 'gas', label: '도시가스', amount: 12400, icon: IconFlame, color: 'text-[#ee8a20]' },
]

function editResult() {
  store.saveBillDraft({ billingMonth: targetYearMonth.value, items: recognizedItems })
  router.push({ path: '/analysis/bills/new', query: { month: targetYearMonth.value, mode: 'manual' } })
}
</script>

<template>
  <AppSubLayout back="/analysis/bills/new">
    <header class="mb-5">
      <h1 class="text-title text-ink mt-1 mb-1">인식 결과를 확인해 주세요</h1>
      <p class="text-caption text-muted m-0">고지서에서 읽은 항목을 확인했어요.</p>
    </header>

    <section class="bg-surface rounded-xl p-5 shadow-sm">
      <div class="mb-5 flex items-center justify-between">
        <div>
          <p class="text-caption text-muted mt-0 mb-1">인식된 고지서</p>
          <h2 class="text-section text-ink m-0">{{ formatMonthOnly(targetYearMonth) }} 생활비 고지서</h2>
        </div>
        <span class="bg-primary-bg text-primary flex size-8 items-center justify-center rounded-full">
          <IconCheck :size="18" />
        </span>
      </div>

      <ul class="m-0 list-none p-0">
        <li
          v-for="item in recognizedItems"
          :key="item.type"
          class="border-divider flex min-h-14 items-center gap-3 border-t first:border-t-0"
        >
          <component :is="item.icon" :size="20" :class="item.color" />
          <span class="text-body-sm text-ink flex-1">{{ item.label }}</span>
          <strong class="text-body-strong text-ink tabular-nums">{{ formatWon(item.amount) }}</strong>
        </li>
      </ul>
    </section>

    <GpButton class="mt-5" @click="editResult">인식 내용 수정하기</GpButton>
  </AppSubLayout>
</template>
