<script setup>
/*
 * 혜택 탭 아이콘 — 선물 상자
 * 뚜껑/몸통을 가로 틈으로, 좌/우를 세로 틈으로 나눠 리본이 지나가는 것처럼 보이게 한다.
 * 왼쪽 칸이 좁고 오른쪽 칸이 넓은 비대칭이 시안 형태다.
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
    <!-- 리본 꼬리 -->
    <path
      d="M11.4 4.4 14.2 1.6"
      fill="none"
      stroke="currentColor"
      stroke-width="2.3"
      stroke-linecap="round"
    />
    <!-- 뚜껑 -->
    <rect x="3" y="4.4" width="6.8" height="5.8" rx="1.3" />
    <rect x="11.6" y="4.4" width="9.4" height="5.8" rx="1.3" />
    <!-- 몸통 -->
    <rect x="3" y="11.9" width="6.8" height="8.7" rx="1.3" />
    <rect x="11.6" y="11.9" width="9.4" height="8.7" rx="1.3" />
  </svg>
</template>
