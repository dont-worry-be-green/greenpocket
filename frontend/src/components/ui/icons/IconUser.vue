<script setup>
/* 마이페이지 탭 아이콘 — 사람 */
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
    <circle cx="12" cy="7.4" r="4.2" />
    <path
      d="M12 13.6c-4.5 0-8.1 2.8-8.5 6.5-.1.9.6 1.7 1.5 1.7h14c.9 0 1.6-.8 1.5-1.7-.4-3.7-4-6.5-8.5-6.5Z"
    />
  </svg>
</template>
