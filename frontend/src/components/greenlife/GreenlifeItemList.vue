<script setup>
/*
 * 「실천 항목」 카드 (C-2-03 · BN-02 · 결정 C-32)
 *
 * 제목 옆에 총 개수(17개), 행은 `GreenlifeItemRow`, 접힌 뒤 「17개 전체 보기 ⌄」 · 펼친 뒤 「접기 ⌃」.
 *
 * ── 접는 개수를 화면이 정하지 않는다 ────────────────────────────────────
 * `collapsedAfter` 는 서버가 주는 FE 힌트다(api-spec 12.3). 상수로 박으면 서버가 힌트를 바꿔도
 * 화면만 옛날 값으로 남는다. 힌트가 없거나 총 개수보다 크면 접지 않는다.
 * 「전체 보기」는 이미 받아 둔 배열을 펴는 것뿐이라 API 를 다시 부르지 않는다.
 */
import { computed, ref } from 'vue'

import GreenlifeItemRow from './GreenlifeItemRow.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconCaretDown from '@/components/ui/icons/IconCaretDown.vue'

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
  <GpCard title="실천 항목">
    <template #action>
      <span class="text-list-title text-ink tabular-nums">
        {{ total }}<span class="text-caption text-muted ml-px font-medium">개</span>
      </span>
    </template>

    <ul class="divide-divider -mt-1 m-0 list-none divide-y p-0">
      <li v-for="item in visibleItems" :key="item.itemId">
        <GreenlifeItemRow :item="item" @select="$emit('select', $event)" />
      </li>
    </ul>

    <button
      v-if="collapsible"
      type="button"
      class="text-caption text-muted border-divider mt-1 flex w-full cursor-pointer items-center justify-center gap-1 border-0 border-t bg-transparent pt-3 font-bold"
      :aria-expanded="expanded"
      @click="expanded = !expanded"
    >
      {{ expanded ? '접기' : `${total}개 전체 보기` }}
      <IconCaretDown :size="12" :class="expanded && 'rotate-180'" />
    </button>
  </GpCard>
</template>
