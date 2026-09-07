<script setup>
/*
 * 보관함 바로가기 2장 — MY-01 (E-1-02)
 *
 * 시안은 두 카드를 초록/파랑으로 갈라 놓았지만 **파란 계열 토큰은 「수도」 전용**이다
 * (`--color-water`). 의미가 어긋난 색을 쓰지 않고 둘 다 브랜드 초록으로 둔다. 카드가
 * 구분되는 근거는 색이 아니라 아이콘과 제목이다.
 */
import GpCard from '@/components/ui/GpCard.vue'
import IconChart from '@/components/ui/icons/IconChart.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'

defineEmits(['monthly', 'eco'])

const CARDS = [
  {
    key: 'monthly',
    title: '월별 리포트',
    description: '매달 생활비 리포트를 확인해보세요.',
    icon: IconChart,
  },
  {
    key: 'eco',
    title: 'ECO 리포트',
    description: '6개월 실제 평가 결과를 확인해보세요.',
    icon: IconLeaf,
  },
]
</script>

<template>
  <div class="grid grid-cols-2 gap-3">
    <GpCard v-for="card in CARDS" :key="card.key">
      <div class="flex items-start justify-between gap-2">
        <div class="min-w-0">
          <p class="text-list-title text-primary-on-soft m-0">{{ card.title }}</p>
          <p class="text-caption text-muted mt-1.5 mb-0">{{ card.description }}</p>
        </div>
        <span
          class="bg-primary-bg text-primary-soft flex size-(--gp-tile-sm) shrink-0 items-center justify-center rounded-md"
          aria-hidden="true"
        >
          <component :is="card.icon" :size="18" />
        </span>
      </div>

      <button
        type="button"
        class="border-divider text-body-strong text-primary-on-soft mt-3 flex w-full cursor-pointer items-center justify-between border-0 border-t bg-transparent pt-3"
        @click="$emit(card.key)"
      >
        바로가기
        <IconChevronRight :size="16" />
      </button>
    </GpCard>
  </div>
</template>
