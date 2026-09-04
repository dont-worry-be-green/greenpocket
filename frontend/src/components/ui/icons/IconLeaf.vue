<script setup>
/*
 * What-if 탭 아이콘 — 잎사귀. 탭바 가운데 FAB 안에 흰색으로 들어간다.
 *
 * 잎맥은 선을 덧그리지 않고 fill-rule="evenodd" 로 뚫는다.
 * 흰 잎 위에 흰 선을 그을 수 없어서인데, 구멍이라 뒤의 그라데이션이 그대로 비친다.
 */
defineProps({ size: { type: [Number, String], default: 24 } })
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="currentColor"
    fill-rule="evenodd"
    aria-hidden="true"
    focusable="false"
  >
    <path
      d="M20.8 3.2c.7 8.4-1.6 13.6-5.8 16.2-3.8 2.3-8.3 1.4-10.1-1.5C3 15 4.2 10.9 7.7 7.9c3.3-2.8 7.8-4.4 13.1-4.7ZM17.9 6.6c-4.6 2.2-8.1 5.7-10.4 10.4l1.4.7c2.1-4.4 5.4-7.6 9.7-9.7l-.7-1.4Z"
    />
  </svg>
</template>
