<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import billIcon from '@/assets/icons/bill.svg'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'

const route = useRoute()
const router = useRouter()
let resultTimer

onMounted(() => {
  resultTimer = window.setTimeout(() => {
    router.replace({ path: '/analysis/bills/result', query: { month: route.query.month } })
  }, 1400)
})

onBeforeUnmount(() => window.clearTimeout(resultTimer))
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
      <h1 class="text-title text-ink mt-0 mb-2">고지서를 읽고 있어요</h1>
      <p class="text-body-sm text-muted m-0">잠시만 기다려 주세요.</p>
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
