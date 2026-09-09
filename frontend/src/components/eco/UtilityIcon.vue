<script setup>
/*
 * 요금 종류 타일 — WF-02(연동 중)·WF-03(기준 사용량)·WF-04(목표 카드·요금 세그먼트)가 같은 모양을 쓴다.
 * utilityType 은 api-spec.md 3절 UtilityType enum (ELECTRICITY · GAS · WATER).
 * 한국어 이름은 utils/format.js 의 formatUtilityType 이 맡는다.
 *
 * ── 그림은 디자이너가 준 PNG 다 (2026-09-09 수현) ─────────────────────────
 * `assets/icons/{electric,gas,water}.png`(1254px 원본)에서 타일 경계로 잘라 144px(48px 의 3배)로 줄이고
 * 64색 팔레트로 뽑은 `*-144.png`(각 2KB 미만)를 쓴다. 바탕 타일·모서리·색이 그림 안에 들어 있어
 * 여기서 배경·글자색 토큰을 입히지 않는다 — 타일 색은 elec-bg · gas-bg · water-bg 토큰값과 같다.
 * 원본을 바꾸면 같은 방법으로 다시 뽑는다(PIL: 알파 ≥ 24 bbox → 정사각 crop → LANCZOS 144 → quantize 64).
 * 진단(고지서 직접 입력·인식 결과)·마이(보관함)도 같은 PNG 를 직접 import 한다 — 도메인 경계 때문에 이 컴포넌트를
 * 넘겨 쓰지 않는다. SVG 글리프(ui/icons 의 IconLightning · IconFlame · IconDrop)는 이제 쓰는 곳이 없다.
 */
import electric from '@/assets/icons/electric-144.png'
import gas from '@/assets/icons/gas-144.png'
import water from '@/assets/icons/water-144.png'

defineProps({
  utilityType: { type: String, required: true },
  // WF-06 목표 카드의 3열처럼 좁은 자리용. 기본 48px 타일은 3열에 들어가지 않는다
  small: { type: Boolean, default: false },
  // WF-04 요금 세그먼트·목표 카드 행처럼 글자 옆 인라인용 24px. small 보다 우선한다
  xs: { type: Boolean, default: false },
})

const SRC = { ELECTRICITY: electric, GAS: gas, WATER: water }
</script>

<template>
  <img
    :src="SRC[utilityType]"
    alt=""
    aria-hidden="true"
    draggable="false"
    class="block shrink-0 select-none"
    :class="xs ? 'size-6' : small ? 'size-(--gp-tile-sm)' : 'size-(--gp-tile)'"
  />
</template>
