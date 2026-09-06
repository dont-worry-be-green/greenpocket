<script setup>
/*
 * 보관함 목록의 요금 아이콘 — MY-03
 *
 * `eco/UtilityIcon.vue` 와 매핑이 같지만 **모양이 다르다** — 시안 MY-03 은 목록 행마다
 * 원형이고, eco 쪽은 rounded-md 타일이다. 도메인을 넘어 import 하지 않는 규칙과 겹쳐
 * `GreenlifeState`/`PocketState` 처럼 복제했다. 색 토큰은 같은 것을 쓴다.
 */
import IconDrop from '@/components/ui/icons/IconDrop.vue'
import IconFlame from '@/components/ui/icons/IconFlame.vue'
import IconLightning from '@/components/ui/icons/IconLightning.vue'

defineProps({
  utilityType: { type: String, required: true },
})

const UTILITY = {
  ELECTRICITY: { icon: IconLightning, tone: 'bg-elec-bg text-elec' },
  GAS: { icon: IconFlame, tone: 'bg-gas-bg text-gas' },
  WATER: { icon: IconDrop, tone: 'bg-water-bg text-water' },
}
</script>

<template>
  <span
    class="flex size-(--gp-tile) shrink-0 items-center justify-center rounded-full"
    :class="UTILITY[utilityType]?.tone"
    aria-hidden="true"
  >
    <component :is="UTILITY[utilityType]?.icon" :size="20" />
  </span>
</template>
