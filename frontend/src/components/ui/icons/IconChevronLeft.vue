<script setup>
/* 하위 화면 뒤로가기 헤더용 꺾쇠 (COM-02) */
defineProps({ size: { type: [Number, String], default: 24 } })
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path d="M15 4.5 7.5 12l7.5 7.5" />
  </svg>
</template>
