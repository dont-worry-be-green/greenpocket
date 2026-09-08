<script setup>
import KbBankWordmark from '@/components/pocket/KbBankWordmark.vue'

defineProps({
  product: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: Object, default: null },
})

defineEmits(['view', 'retry'])
</script>

<template>
  <section
    v-if="loading"
    class="bg-skeleton h-16 animate-pulse rounded-lg"
    aria-label="추천 상품 불러오는 중"
  />

  <section v-else-if="error" class="bg-surface flex min-h-16 items-center gap-3 rounded-lg px-4 py-3">
    <p class="text-caption text-muted m-0 min-w-0 flex-1">추천 상품을 불러오지 못했어요</p>
    <button
      type="button"
      class="text-caption text-ink-soft shrink-0 border-0 bg-transparent p-2 font-semibold"
      @click="$emit('retry')"
    >
      다시 시도
    </button>
  </section>

  <section
    v-else-if="product"
    class="bg-surface flex min-h-16 items-center gap-3 rounded-lg px-4 py-2.5 shadow-sm"
    aria-label="추천 금융 상품"
  >
    <KbBankWordmark compact :show-label="false" class="shrink-0" />
    <strong class="text-list-title text-ink min-w-0 flex-1 break-keep">
      {{ product.name }}
    </strong>
    <button
      type="button"
      class="kb-product-button text-caption shrink-0 rounded-sm border-0 px-3 py-2 font-semibold"
      @click="$emit('view')"
    >
      상품 보기
    </button>
  </section>
</template>

<style scoped>
.kb-product-button {
  background: #f4a900;
  color: #1a1a1a;
}

.kb-product-button:active {
  background: #d99500;
}
</style>
