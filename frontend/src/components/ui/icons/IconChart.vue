<script setup>
/*
 * 진단 탭 아이콘 — 막대그래프
 * 시안(연동전.png) 탭바 기준: 짧은 막대 · 가장 높은 막대 · 중간 막대 순서다.
 */
defineProps({ size: { type: [Number, String], default: 24 } })
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="currentColor"
    aria-hidden="true"
    focusable="false"
  >
    <rect x="3.4" y="13.2" width="4" height="7.4" rx="1.6" />
    <rect x="10" y="4.6" width="4" height="16" rx="1.6" />
    <rect x="16.6" y="8.6" width="4" height="12" rx="1.6" />
  </svg>
</template>
