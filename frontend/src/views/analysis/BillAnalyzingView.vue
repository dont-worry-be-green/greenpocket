<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import billIcon from '@/assets/icons/bill.svg'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useAnalysisStore } from '@/stores/analysis'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()
const progress = computed(() => Math.min(100, Math.max(0, store.ocrProgress ?? 0)))

async function analyze() {
  const result = await store.analyzeSelectedImage(route.query.month)
  if (result) {
    router.replace({ path: '/analysis/bills/result', query: { month: route.query.month } })
  }
}

function useManualEntry() {
  router.replace({
    path: '/analysis/bills/new',
    query: { month: route.query.month, mode: 'manual' },
  })
}

onMounted(analyze)
</script>

<template>
  <AppSubLayout back="/analysis/bills/new">
    <section class="flex min-h-[65dvh] flex-col items-center justify-center text-center">
      <div
        class="bg-primary-bg relative mb-7 flex size-36 items-center justify-center overflow-hidden rounded-xl"
        aria-hidden="true"
      >
        <img :src="billIcon" alt="" class="h-14 w-14" />
        <span class="analysis-scan-line absolute left-6 h-0.5 w-24 rounded-full" />
      </div>
      <template v-if="!store.ocrError">
        <h1 class="text-title text-ink mt-0 mb-2">고지서를 읽고 있어요</h1>
        <p class="text-body-sm text-muted mt-0 mb-5">잠시만 기다려 주세요.</p>
        <div class="bg-disabled-bg h-2 w-full max-w-64 overflow-hidden rounded-full">
          <span
            class="bg-primary block h-full rounded-full transition-[width] duration-300"
            :style="{ width: `${progress}%` }"
          />
        </div>
        <strong class="text-caption text-primary mt-2">{{ progress }}%</strong>
      </template>
      <template v-else>
        <h1 class="text-title text-ink mt-0 mb-2">고지서를 읽지 못했어요</h1>
        <p class="text-body-sm text-muted mt-0 mb-6">{{ store.ocrError.message }}</p>
        <div class="grid w-full max-w-72 grid-cols-2 gap-3">
          <button
            type="button"
            class="border-control-border bg-surface text-ink h-(--gp-cta-h) rounded-md border text-button"
            @click="useManualEntry"
          >
            직접 입력
          </button>
          <GpButton @click="analyze">다시 시도</GpButton>
        </div>
      </template>
    </section>
  </AppSubLayout>
</template>

<style scoped>
.analysis-scan-line {
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  box-shadow: 0 0 10px color-mix(in srgb, var(--color-primary) 45%, transparent);
  animation: analyze-bill 1.2s ease-in-out infinite alternate;
}

@keyframes analyze-bill {
  from { transform: translateY(-42px); }
  to { transform: translateY(42px); }
}

@media (prefers-reduced-motion: reduce) {
  .analysis-scan-line { animation: none; }
}
</style>
