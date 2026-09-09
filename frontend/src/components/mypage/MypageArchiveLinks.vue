<script setup>
/*
 * 보관함 바로가기 2장 — MY-01 (E-1-02)
 */
import GpCard from '@/components/ui/GpCard.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'

defineEmits(['monthly', 'eco'])

const CARDS = [
  { key: 'monthly', title: '월별 리포트' },
  { key: 'eco', title: 'ECO 리포트' },
]
</script>

<template>
  <div class="grid grid-cols-2 gap-3">
    <GpCard v-for="card in CARDS" :key="card.key">
      <p class="text-list-title text-primary-on-soft m-0">{{ card.title }}</p>

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
