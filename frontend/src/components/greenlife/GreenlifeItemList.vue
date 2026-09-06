<script setup>
/*
 * 전체 실천 항목 (C-2-03 · BN-02)
 *
 * ── 접는 개수를 화면이 정하지 않는다 ────────────────────────────────────
 * `collapsedAfter` 는 서버가 주는 FE 힌트다(api-spec 12.3). 6을 여기 상수로 박으면 서버가
 * 힌트를 바꿔도 화면만 옛날 값으로 남는다. 힌트가 없거나 총 개수보다 크면 접지 않는다.
 *
 * 「전체 보기」는 이미 받아 둔 배열을 펴는 것뿐이라 API 를 다시 부르지 않는다 —
 * 12.3 은 항상 17개를 통째로 내려준다.
 */
import { computed, ref } from 'vue'

import GreenlifeItemRow from './GreenlifeItemRow.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  totalCount: { type: Number, default: 0 },
  collapsedAfter: { type: Number, default: null },
})
defineEmits(['select'])

const expanded = ref(false)

const collapsible = computed(() => {
  const after = Number(props.collapsedAfter)
  return Number.isInteger(after) && after > 0 && after < props.items.length
})

const visibleItems = computed(() =>
  collapsible.value && !expanded.value ? props.items.slice(0, props.collapsedAfter) : props.items,
)

const total = computed(() => props.totalCount || props.items.length)
</script>

<template>
  <GpCard title="전체 실천 항목" caption="항목을 누르면 실천 방법과 적립 내역을 볼 수 있어요">
    <template #action>
      <GpTag tone="primary">{{ total }}개</GpTag>
    </template>

    <ul class="divide-divider m-0 list-none divide-y p-0">
      <li v-for="item in visibleItems" :key="item.itemId">
        <GreenlifeItemRow :item="item" @select="$emit('select', $event)" />
      </li>
    </ul>

    <button
      v-if="collapsible && !expanded"
      type="button"
      class="text-body-sm text-ink-soft border-divider mt-1 flex w-full items-center justify-center gap-1 border-t pt-3"
      @click="expanded = true"
    >
      아래로 내려 {{ total }}개 전체 보기
      <IconChevronDown :size="14" class="text-muted" />
    </button>
  </GpCard>
</template>
