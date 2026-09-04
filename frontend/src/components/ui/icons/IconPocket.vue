<script setup>
/*
 * 포켓 탭 아이콘 — 뚜껑 달린 보관함
 * 가운데 손잡이는 fill-rule="evenodd" 로 뚫어서 배경이 비치게 한다.
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
    <rect x="3.2" y="3.4" width="17.6" height="4.6" rx="1.6" />
    <path
      fill-rule="evenodd"
      d="M6.4 9.2h11.2a2.6 2.6 0 0 1 2.6 2.6V18a2.6 2.6 0 0 1-2.6 2.6H6.4A2.6 2.6 0 0 1 3.8 18v-6.2a2.6 2.6 0 0 1 2.6-2.6Zm5.6 2.4a2.3 2.3 0 1 0 0 4.6 2.3 2.3 0 0 0 0-4.6Z"
    />
  </svg>
</template>
