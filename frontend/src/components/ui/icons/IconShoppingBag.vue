<script setup>
/* Phosphor PhShoppingBag (fill) — scripts/add-icon.mjs 로 추출 */
defineProps({ size: { type: [Number, String], default: 24 } })
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 256 256"
    fill="currentColor"
    aria-hidden="true"
    focusable="false"
  >
    <path
      d="M216,40H40A16,16,0,0,0,24,56V200a16,16,0,0,0,16,16H216a16,16,0,0,0,16-16V56A16,16,0,0,0,216,40Zm-88,96A48.05,48.05,0,0,1,80,88a8,8,0,0,1,16,0,32,32,0,0,0,64,0,8,8,0,0,1,16,0A48.05,48.05,0,0,1,128,136Z"
    />
  </svg>
</template>
