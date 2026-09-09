<script setup>
/*
 * 실천 항목 아이콘 (C-1-03) — 서버의 `iconKey` 를 아이콘 컴포넌트로 옮긴다.
 *
 * 17개 항목에 각자 `iconKey` 가 있지만 **시안에 실제로 보이는 6개만 뽑아 두었다.**
 * 나머지는 폴백(`IconPlant`)으로 떨어진다 — 목록이 비지 않고, 나중에 아이콘을 더 뽑으면
 * 아래 맵에 한 줄씩 더하는 것으로 끝난다.
 *
 * **모르는 `iconKey` 를 에러로 만들지 않는다.** 서버가 항목을 늘리면 화면이 먼저 깨진다.
 */
import IconContainer from '@/components/ui/icons/IconContainer.vue'
import IconCupReturn from '@/components/ui/icons/IconCupReturn.vue'
import IconPlant from '@/components/ui/icons/IconPlant.vue'
import IconReceipt from '@/components/ui/icons/IconReceipt.vue'
import IconRefill from '@/components/ui/icons/IconRefill.vue'
import IconShoppingBag from '@/components/ui/icons/IconShoppingBag.vue'
import IconTumbler from '@/components/ui/icons/IconTumbler.vue'

const props = defineProps({
  iconKey: { type: String, default: '' },
  size: { type: [Number, String], default: 22 },
  /** 미실천 항목. 타일을 회색으로 떨어뜨려 실천한 줄이 먼저 보이게 한다(결정 C-32) */
  muted: { type: Boolean, default: false },
})

// 키는 db/seed/greenlife_items.sql 의 icon_key 다
const ICONS = {
  receipt: IconReceipt,
  tumbler: IconTumbler,
  container: IconContainer,
  refill: IconRefill,
  'shopping-bag': IconShoppingBag,
  'cup-return': IconCupReturn,
}

const icon = () => ICONS[props.iconKey] ?? IconPlant
</script>

<template>
  <span
    class="flex size-10 flex-none items-center justify-center rounded-md"
    :class="muted ? 'bg-surface-sub text-icon-off' : 'bg-primary-bg text-primary-soft'"
  >
    <component :is="icon()" :size="size" />
  </span>
</template>
