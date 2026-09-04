<script setup>
/*
 * 요금 종류 타일 — WF-02(연동 중)·WF-03(기준 사용량)이 같은 모양을 쓴다.
 * utilityType 은 api-spec.md 3절 UtilityType enum (ELECTRICITY · GAS · WATER).
 * 한국어 이름은 utils/format.js 의 formatUtilityType 이 맡는다.
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
    class="flex size-(--gp-tile) shrink-0 items-center justify-center rounded-md"
    :class="UTILITY[utilityType]?.tone"
    aria-hidden="true"
  >
    <component :is="UTILITY[utilityType]?.icon" :size="22" />
  </span>
</template>
