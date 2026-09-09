<script setup>
/*
 * WF-02 사용량 불러오는 중 (B-1-03)
 *
 * utilities 는 GET /eco/link/{linkJobId} 의 utilityStatus 형태다.
 * API가 내려 주는 세 상태(PENDING · RUNNING · SUCCEEDED)를 화면의 세 데이터 카드에
 * 같은 순서로 반영해 로딩 진행을 보여 준다.
 */
import { computed } from 'vue'

import ecoMileageLogo from '@/assets/eco-mileage-logo.png'
import IconChart from '@/components/ui/icons/IconChart.vue'
import IconCheck from '@/components/ui/icons/IconCheck.vue'
import IconMapPin from '@/components/ui/icons/IconMapPin.vue'
import IconReceipt from '@/components/ui/icons/IconReceipt.vue'

const props = defineProps({
  utilities: { type: Array, required: true },
})

const DATA_STAGES = [
  { title: '요금 종류', description: '전기·가스·수도', icon: IconReceipt },
  { title: '2년 사용량', description: '평가 기준', icon: IconChart },
  { title: '등록 주소', description: '에코마일리지', icon: IconMapPin },
]

const STATUS_LABEL = {
  PENDING: '대기 중',
  RUNNING: '불러오는 중',
  SUCCEEDED: '불러오기 완료',
}

const stages = computed(() => {
  const hasProgress = props.utilities.some((item) => item.status !== 'PENDING')

  return DATA_STAGES.map((stage, index) => ({
    ...stage,
    status: props.utilities[index]?.status ?? (!hasProgress && index === 0 ? 'RUNNING' : 'PENDING'),
  }))
})
</script>

<template>
  <div class="space-y-7 pt-28">
    <section class="flex items-center justify-center gap-2" role="status" aria-live="polite">
      <h2 class="text-section m-0 font-semibold">에코마일리지와 연동 중이에요</h2>
      <span
        class="border-track border-t-primary size-4 animate-spin rounded-full border-2"
        aria-hidden="true"
      />
    </section>

    <section>
      <div class="mb-3 flex items-center gap-2">
        <h3 class="text-list-title m-0 font-semibold">불러올 데이터</h3>
        <img
          :src="ecoMileageLogo"
          alt="에코마일리지"
          class="h-8 w-auto max-w-28 object-contain mix-blend-multiply"
        />
      </div>

      <ul class="m-0 grid list-none grid-cols-3 gap-2 p-0">
        <li
          v-for="(stage, index) in stages"
          :key="stage.title"
          class="stage-card bg-surface rounded-card relative flex min-h-40 flex-col items-center overflow-hidden px-2 py-5 text-center"
          :class="`stage-card--${stage.status.toLowerCase()}`"
          :style="{ '--stage-delay': `${index * 110}ms` }"
        >
          <span v-if="stage.status === 'RUNNING'" class="stage-sheen" aria-hidden="true" />

          <span class="absolute top-2.5 right-2.5 z-[1] flex size-5 items-center justify-center">
            <span
              v-if="stage.status === 'SUCCEEDED'"
              class="stage-check bg-primary text-primary-fg flex size-5 items-center justify-center rounded-full"
            >
              <IconCheck :size="14" />
            </span>
            <span
              v-else-if="stage.status === 'RUNNING'"
              class="border-track border-t-primary size-4 animate-spin rounded-full border-2"
              aria-hidden="true"
            />
            <span v-else class="border-track size-4 rounded-full border-2" aria-hidden="true" />
          </span>

          <span
            class="bg-primary-bg text-primary-on-soft relative z-[1] flex size-12 items-center justify-center rounded-full"
          >
            <component :is="stage.icon" :size="23" />
          </span>
          <span class="text-body relative z-[1] mt-4 font-semibold">{{ stage.title }}</span>
          <span class="text-caption text-muted relative z-[1] mt-1">{{ stage.description }}</span>
          <span class="sr-only">{{ STATUS_LABEL[stage.status] }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.stage-card {
  animation: stage-enter 360ms ease-out both;
  animation-delay: var(--stage-delay);
  transition:
    box-shadow 240ms ease,
    transform 240ms ease;
}

.stage-card--running {
  box-shadow: 0 8px 24px rgb(0 133 75 / 10%);
}

.stage-sheen {
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 20%, rgb(223 244 234 / 52%) 48%, transparent 76%);
  transform: translateX(-100%);
  animation: stage-sheen 1.35s ease-in-out infinite;
}

.stage-check {
  animation: stage-check 260ms ease-out both;
}

@keyframes stage-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes stage-sheen {
  to {
    transform: translateX(100%);
  }
}

@keyframes stage-check {
  from {
    opacity: 0;
    transform: scale(0.6);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .stage-card,
  .stage-sheen,
  .stage-check {
    animation: none;
  }
}
</style>
