<script setup>
/*
 * 온보딩 · 단일 선택 라디오 리스트 (A-1-03 평수 구간)
 *
 * 칩(`OnbChoiceChips`)과 동작이 같고 **생김새만 다르다.** 시안이 평수만 세로 리스트라
 * 한 컴포넌트에 `variant` 를 두는 대신 파일을 나눴다 — 선택 표시(라디오 원)가 칩에는 없다.
 *
 * v-model 은 ENUM 코드 문자열이다(`UNDER_10`). 서버로 그대로 보낸다.
 */
defineProps({
  modelValue: { type: String, default: null },
  /** [{ value: 'UNDER_10', label: '10평 이하' }] */
  options: { type: Array, required: true },
  label: { type: String, required: true },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div>
    <span class="text-body-strong text-muted mb-3 block">{{ label }}</span>
    <div class="space-y-2" role="radiogroup" :aria-label="label">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        role="radio"
        :aria-checked="option.value === modelValue"
        class="ease-standard text-body flex min-h-14 w-full cursor-pointer items-center gap-3 rounded-lg border px-4 text-left transition-colors duration-140"
        :class="
          option.value === modelValue
            ? 'bg-primary-bg border-primary text-ink font-semibold'
            : 'bg-surface border-border text-ink-soft'
        "
        @click="$emit('update:modelValue', option.value)"
      >
        <span
          class="flex size-(--gp-checkbox-sm) shrink-0 items-center justify-center rounded-full border-2"
          :class="option.value === modelValue ? 'border-primary' : 'border-control-off'"
          aria-hidden="true"
        >
          <span v-if="option.value === modelValue" class="bg-primary size-2.5 rounded-full" />
        </span>
        {{ option.label }}
      </button>
    </div>
  </div>
</template>
