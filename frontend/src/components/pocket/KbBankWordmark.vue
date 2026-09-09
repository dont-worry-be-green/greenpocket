<script setup>
defineProps({
  compact: { type: Boolean, default: false },
  showLabel: { type: Boolean, default: true },
})
</script>

<template>
  <span class="inline-flex items-center gap-1.5" aria-label="KB국민은행">
    <svg
      :class="compact ? 'h-5 w-8' : 'h-6 w-10'"
      viewBox="0 0 48 30"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M18 2v26M18 15 4 6m14 9L3 23m15-8 13-12m-13 12 15 11"
        stroke="#F4A900"
        stroke-width="3.2"
        stroke-linecap="round"
      />
      <path
        d="M29 8v14c0 3 2 5 5 5 4 0 7-3 7-7s-3-7-7-7c-2 0-4 1-5 3"
        stroke="#F4A900"
        stroke-width="3.2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <strong
      v-if="showLabel"
      :class="compact ? 'text-caption' : 'text-body-sm'"
      class="text-[#71695f] whitespace-nowrap"
    >
      KB국민은행
    </strong>
  </span>
</template>
