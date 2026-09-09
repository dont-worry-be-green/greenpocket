<script setup>
/*
 * 보관함 목록의 요금 아이콘 — MY-03
 *
 * `eco/UtilityIcon.vue` 와 그림(`assets/icons/*-144.png`, 디자이너 PNG)이 같지만 **모양이 다르다** —
 * 시안 MY-03 은 목록 행마다 원형이고, eco 쪽은 타일 그대로다. 그림에 타일 바탕이 들어 있어 원으로
 * 클리핑하면 타일색 원 안에 글리프가 놓인다. 도메인을 넘어 import 하지 않는 규칙과 겹쳐
 * `GreenlifeState`/`PocketState` 처럼 복제했다(2026-09-09 PNG 로 교체).
 */
import electric from '@/assets/icons/electric-144.png'
import gas from '@/assets/icons/gas-144.png'
import water from '@/assets/icons/water-144.png'

defineProps({
  utilityType: { type: String, required: true },
})

const SRC = { ELECTRICITY: electric, GAS: gas, WATER: water }
</script>

<template>
  <img
    :src="SRC[utilityType]"
    alt=""
    aria-hidden="true"
    draggable="false"
    class="block size-(--gp-tile) shrink-0 rounded-full select-none"
  />
</template>
