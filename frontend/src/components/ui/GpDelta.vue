<script setup>
/*
 * 그린포켓 · 증감 표기
 * 부호(−12%)는 늘었다는 건지 줄었다는 건지 헷갈린다.
 * 화살표 + 숫자 + 말 세 겹으로 방향을 못 박는다. 색만으로 구분하지 않는다.
 *
 * 방향과 자릿수 판단은 utils/format.js 의 changeRateParts 한 곳에만 있다.
 * (frontend/AGENTS.md 6절 — 컴포넌트가 숫자 포맷을 직접 만들지 않는다)
 *   value > 0 : 줄었다   value < 0 : 늘었다   value = 0 : 지난달과 같아요
 *   11.322 를 11 로 깎지 않는다 (결정 C-13)
 */
import { computed } from 'vue'
import { changeRateParts } from '@/utils/format'

const props = defineProps({
  value: { type: Number, default: null },
  size: { type: String, default: 'md' }, // sm(14) | md(17) | lg(24)
  word: { type: String, default: '' }, // '줄여야 해요' 처럼 문구를 갈아끼울 때
  showWord: { type: Boolean, default: true },
})

const parts = computed(() => changeRateParts(props.value))

const SIZE = { sm: 'text-label', md: 'text-list-title', lg: 'text-amount' }
const sizeClass = computed(() => SIZE[props.size] ?? SIZE.md)
</script>

<template>
  <span v-if="parts.direction === 'none'" class="text-muted">-</span>

  <!-- 0 은 화살표를 쓰지 않는다. ↓0% 는 줄었다는 뜻으로 읽힌다 -->
  <span v-else-if="parts.direction === 'same'" class="text-muted" :class="sizeClass">
    {{ word || parts.word }}
  </span>

  <span v-else class="inline-flex items-baseline gap-1.5">
    <span
      class="inline-flex items-center gap-[3px] font-bold tabular-nums tracking-body"
      :class="[sizeClass, parts.direction === 'down' ? 'text-decrease' : 'text-increase']"
    >
      <svg class="size-[1em] flex-none" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path
          v-if="parts.direction === 'down'"
          d="M11 3h2v12.6l4.3-4.3 1.4 1.4L12 19.4l-6.7-6.7 1.4-1.4L11 15.6z"
        />
        <path v-else d="M13 21h-2V8.4l-4.3 4.3-1.4-1.4L12 4.6l6.7 6.7-1.4 1.4L13 8.4z" />
      </svg>
      {{ parts.value }}%
    </span>
    <span v-if="showWord" class="text-caption text-ink-soft font-semibold">
      {{ word || parts.word }}
    </span>
  </span>
</template>
