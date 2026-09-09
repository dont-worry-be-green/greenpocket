<script setup>
/*
 * 은행 로고 타일. 이미지가 있는 은행(KB국민은행)은 로고를, 없으면 색 바탕에 짧은 이름을 그린다.
 * 메타는 `bankMeta.js` 하나다 — 잔액 카드의 「국민」 글자와 같은 표를 본다.
 */
import { computed } from 'vue'

import { bankMeta } from './bankMeta'

const props = defineProps({
  bankCode: { type: String, default: null },
  bankName: { type: String, default: null },
  size: { type: Number, default: 36 },
})

const meta = computed(() => bankMeta(props.bankCode, props.bankName))
// 시안 실측 — 40px 타일에 12px 모서리. 크기가 바뀌어도 같은 비율을 지킨다
const radius = computed(() => `${Math.round(props.size * 0.3)}px`)
</script>

<template>
  <span
    class="inline-flex shrink-0 items-center justify-center overflow-hidden font-bold tracking-[-0.02em]"
    :style="{
      width: size + 'px',
      height: size + 'px',
      background: meta.bg,
      color: meta.color,
      fontSize: size * 0.3 + 'px',
      borderRadius: radius,
      lineHeight: 1,
    }"
    aria-hidden="true"
  >
    <img v-if="meta.image" :src="meta.image" alt="" class="block size-full object-cover" />
    <template v-else>{{ meta.label }}</template>
  </span>
</template>
