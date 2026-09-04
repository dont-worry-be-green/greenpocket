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
  // WF-06 목표 카드의 3열처럼 좁은 자리용. 기본 48px 타일은 3열에 들어가지 않는다
  small: { type: Boolean, default: false },
})

const UTILITY = {
  ELECTRICITY: { icon: IconLightning, tone: 'bg-elec-bg text-elec' },
  GAS: { icon: IconFlame, tone: 'bg-gas-bg text-gas' },
  WATER: { icon: IconDrop, tone: 'bg-water-bg text-water' },
}
</script>

<template>
  <span
    class="flex shrink-0 items-center justify-center rounded-md"
    :class="[UTILITY[utilityType]?.tone, small ? 'size-(--gp-tile-sm)' : 'size-(--gp-tile)']"
    aria-hidden="true"
  >
    <component :is="UTILITY[utilityType]?.icon" :size="small ? 18 : 22" />
  </span>
</template>
